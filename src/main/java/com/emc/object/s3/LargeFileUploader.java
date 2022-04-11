/*
 * Copyright (c) 2015-2016, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3;

import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.lfu.*;
import com.emc.object.s3.request.*;
import com.emc.object.util.ProgressInputStream;
import com.emc.object.util.ProgressListener;
import com.emc.rest.util.SizedInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Convenience class to facilitate multipart upload for large files. This class will split the file
 * and upload it in parts, transferring several parts simultaneously to maximize efficiency.
 * If any errors occur during the upload, the target object will be deleted (any created MPU will be aborted).
 */
public class LargeFileUploader implements Runnable, ProgressListener {

    private static final Logger log = LoggerFactory.getLogger(LargeFileUploader.class);

    public static final int DEFAULT_THREADS = 8;

    public static final int DEFAULT_MPU_THRESHOLD = 512 * 1024 * 1024; // 512MB

    public static final long MIN_PART_SIZE = 4 * 1024 * 1024; // 4MB
    public static final long DEFAULT_PART_SIZE = 128 * 1024 * 1024; // 128MB
    public static final int MAX_PARTS = 10000;

    public static String getMpuETag(List<MultipartPartETag> partETags) {
        String aggHexString = partETags.stream().map(MultipartPartETag::getETag).collect(Collectors.joining(""));

        byte[] rawBytes = DatatypeConverter.parseHexBinary(aggHexString);

        return DigestUtils.md5Hex(rawBytes) + "-" + partETags.size();
    }

    private final S3Client s3Client;
    private final String bucket;
    private final String key;

    private final InputStream stream;
    private final LargeFileMultipartSource multipartSource;

    private long fullSize;
    private final AtomicLong bytesTransferred = new AtomicLong();
    private String eTag;
    private String versionId;

    private S3ObjectMetadata objectMetadata;
    private AccessControlList acl;
    private CannedAcl cannedAcl;
    private boolean closeStream = true;
    private long mpuThreshold = DEFAULT_MPU_THRESHOLD;
    private Long partSize = DEFAULT_PART_SIZE;
    private int threads = DEFAULT_THREADS;
    private ExecutorService executorService;
    private boolean externalExecutorService;
    private ProgressListener progressListener;
    private final AtomicBoolean active = new AtomicBoolean(false);

    private LargeFileUploaderResumeContext resumeContext;
    private Map<Integer, MultipartPartETag> existingMpuParts = null;
    private boolean abortMpuOnFailure = true;

    /**
     * Creates a new LargeFileUpload instance using the specified <code>s3Client</code> to upload
     * <code>file</code> to <code>bucket/key</code>.
     */
    public LargeFileUploader(S3Client s3Client, String bucket, String key, File file) {
        this(s3Client, bucket, key, new LargeFileMultipartFileSource(file));
    }

    /**
     * Creates a new LargeFileUpload instance using the specified <code>s3Client</code> to upload
     * from a single <code>stream</code> to <code>bucket/key</code>. Note that this type of upload is
     * single-threaded and not very efficient.
     */
    public LargeFileUploader(S3Client s3Client, String bucket, String key, InputStream stream, long size) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.stream = stream;
        this.fullSize = size;
        this.multipartSource = null;
    }

    /**
     * Creates a new LargeFileUpload instance using the specified <code>s3Client</code> to upload
     * from a <code>multipartSource</code> to <code>bucket/key</code>.
     *
     * @see LargeFileMultipartSource
     */
    public LargeFileUploader(S3Client s3Client, String bucket, String key, LargeFileMultipartSource multipartSource) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.multipartSource = multipartSource;
        this.stream = null;
    }

    @Override
    public void progress(long completed, long total) {
    }

    @Override
    public void transferred(long size) {
        long totalTransferred = bytesTransferred.addAndGet(size);

        if (progressListener != null) {
            progressListener.transferred(size);
            progressListener.progress(totalTransferred, fullSize);
        }
    }

    @Override
    public void run() {
        upload();
    }

    protected long getMinPartSize() {
        return MIN_PART_SIZE;
    }

    protected String putObject(InputStream is) {
        PutObjectRequest putRequest = new PutObjectRequest(bucket, key, is);
        putRequest.setObjectMetadata(objectMetadata);
        putRequest.setAcl(acl);
        putRequest.setCannedAcl(cannedAcl);

        PutObjectResult result = s3Client.putObject(putRequest);

        return result.getETag();
    }

    // must return *all* parts (even if list is truncated)
    protected List<MultipartPart> listParts(String uploadId) {
        List<MultipartPart> partList = new ArrayList<>();
        ListPartsRequest request = new ListPartsRequest(bucket, key, uploadId);
        ListPartsResult result = null;
        do {
            if (result != null) request.setMarker(result.getNextPartNumberMarker());
            result = s3Client.listParts(request);
            partList.addAll(result.getParts());
        } while (result.isTruncated());

        return partList;
    }

    protected String initMpu() {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, key);
        initRequest.setObjectMetadata(objectMetadata);
        initRequest.setAcl(acl);
        initRequest.setCannedAcl(cannedAcl);
        return s3Client.initiateMultipartUpload(initRequest).getUploadId();
    }

    protected MultipartPartETag uploadPart(String uploadId, int partNumber, InputStream is, long length) {
        UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNumber, is);
        request.setContentLength(length);

        return s3Client.uploadPart(request);
    }

    protected CompleteMultipartUploadResult completeMpu(String uploadId, SortedSet<MultipartPartETag> parts) {
        CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, uploadId).withParts(parts);
        return s3Client.completeMultipartUpload(compRequest);
    }

    protected void abortMpu(String uploadId) {
        s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
    }

    /**
     * This async version of upload() will start the upload process in the background and immediately return a
     * {@link LargeFileUpload} instance.
     * This allows pausing or aborting the upload in the middle, or you can use <code>waitForCompletion()</code> to
     * block until the upload is complete.
     *
     * @see #upload()
     */
    public LargeFileUpload uploadAsync() {
        // start a background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(this::upload);
        executor.shutdown();

        return new LargeFileUpload() {
            @Override
            public void waitForCompletion() {
                try {
                    future.get();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public LargeFileUploaderResumeContext pause() {
                active.set(false); // all part uploads that have not started yet should effectively become no-ops
                waitForCompletion(); // only waits for parts that are currently uploading
                return resumeContext; // at this point, resumeContext should be accurate
            }

            @Override
            public void abort() {
                active.set(false); // all part uploads that have not started yet should effectively become no-ops
                if (resumeContext != null && resumeContext.getUploadId() != null) {
                    // this should interrupt existing part transfers, but we will not wait for them anyway
                    abortMpu(resumeContext.getUploadId());
                    resumeContext.setUploadId(null);
                    resumeContext.setUploadedParts(null);
                }
                executorService.shutdownNow(); // immediately terminates thread pool and interrupts any running threads
            }
        };
    }

    /**
     * This method will automatically choose between MPU and single-PUT operations based on a configured threshold.
     * Note the default threshold is {@link #DEFAULT_MPU_THRESHOLD}. Also note that the defaults in this class are
     * optimized for high-speed LAN connectivity. When operating over a WAN or a slower connection, you should reduce
     * the {@link #setMpuThreshold(long) MPU threshold} and {@link #setPartSize(long) part size} proportionately.
     */
    public void upload() {
        configure();

        if (fullSize >= mpuThreshold)
            doMultipartUpload();
        else
            doSinglePut();
    }

    private InputStream getSourceCompleteDataStream() throws IOException {
        InputStream is;
        if (multipartSource != null) {
            is = multipartSource.getCompleteDataStream();
        } else {
            // only close the source stream if configured to do so
            is = new FilterInputStream(stream) {
                @Override
                public void close() throws IOException {
                    if (closeStream) super.close();
                    else log.debug("leaving source stream open");
                }
            };
        }

        return is;
    }

    private InputStream getSourcePartDataStream(long offset, long length) throws IOException {
        InputStream is;
        if (multipartSource != null) {
            is = multipartSource.getPartDataStream(offset, length);
        } else {
            // NOTE: this assumes all parts of the source data stream are read in series
            // make sure source stream isn't closed
            is = new FilterInputStream(new SizedInputStream(stream, length)) {
                @Override
                public void close() {
                    // no-op
                }
            };
        }

        return is;
    }

    protected InputStream monitorStream(InputStream stream) {
        return new ProgressInputStream(stream, this);
    }

    public void doSinglePut() {
        configure();

        try (InputStream is = monitorStream(getSourceCompleteDataStream())) {
            eTag = putObject(is);
        } catch (IOException e) {
            throw new RuntimeException("Error opening file", e);
        }
    }

    /*
     * get a map of existing MPU parts from which we can resume an MPU. we can only resume an MPU if the existing
     * part sizes and count are exactly the same as configured in this LFU instance
     */
    private Map<Integer, MultipartPartETag> listUploadPartsForResume(String uploadId) {
        List<MultipartPart> existingParts = listParts(uploadId);
        Map<Integer, MultipartPartETag> partsForResume = new HashMap<>();

        if (existingParts != null) {
            // sort parts based on partNumber
            existingParts.sort(Comparator.comparingInt(MultipartPartETag::getPartNumber));

            // check the parts - if any part size doesn't match, or there are more parts than expected, we cannot resume
            int lastPart = (int) ((fullSize - 1) / partSize) + 1;
            long lastPartSize = fullSize - ((lastPart - 1) * partSize);
            for (MultipartPart part : existingParts) {
                if (part.getPartNumber() > lastPart) {
                    // invalid upload
                    throw new IllegalArgumentException(String.format("Too many parts in uploadId: %s: last part is %d, but saw partNumber %d",
                            uploadId, lastPart, part.getPartNumber()));
                }
                long expectedSize = part.getPartNumber() == lastPart ? lastPartSize : partSize;
                if (!part.getSize().equals(expectedSize)) {
                    // invalid upload
                    throw new IllegalArgumentException(String.format("Invalid part size detected in uploadId: %s/%d: expected %d, but saw %d",
                            uploadId, part.getPartNumber(), expectedSize, part.getSize()));
                }

                // we can skip this part
                partsForResume.put(part.getPartNumber(), part);
            }
        }

        return partsForResume;
    }

    public void doMultipartUpload() {
        configure();

        active.set(true);

        // if calling code has specified a resume context, and did *not* provide a part list, list the parts now
        if (resumeContext != null && resumeContext.getUploadId() != null && resumeContext.getUploadedParts() == null) {
            existingMpuParts = listUploadPartsForResume(resumeContext.getUploadId());
        }

        // always maintain an accurate resume context in case of interruption
        if (resumeContext == null) resumeContext = new LargeFileUploaderResumeContext();

        // initiate MP upload if not resuming
        if (resumeContext.getUploadId() == null) resumeContext.setUploadId(initMpu());

        // make sure trusted part list is initialized (this will be updated as parts are uploaded)
        if (resumeContext.getUploadedParts() == null) resumeContext.setUploadedParts(new HashMap<>());

        List<Future<MultipartPartETag>> futures = new ArrayList<>();
        try {
            // submit all upload tasks
            int lastPart = (int) ((fullSize - 1) / partSize) + 1;
            for (int partNumber = 1; partNumber <= lastPart; partNumber++) {
                long offset = (partNumber - 1) * partSize;
                long length = partSize;
                if (offset + length > fullSize) length = fullSize - offset;

                // if we already have a trusted part ETag, skip this part without verifying
                if (resumeContext.getUploadedParts().containsKey(partNumber)) {
                    log.debug("bucket {} key {} partNumber {} provided in resume context; will use the provided ETag and this part will not be verified",
                            bucket, key, partNumber);

                    // reuse existing MPU parts if found
                } else if (existingMpuParts != null && existingMpuParts.containsKey(partNumber)) {
                    log.debug("bucket {} key {} partNumber {} already exists, will be reused for multipart upload",
                            bucket, key, partNumber);
                    // verify source part if necessary
                    if (resumeContext.isVerifyPartsFoundInTarget()) {
                        futures.add(CompletableFuture // need to use CompletableFuture to allow chained execution
                                // first, verify the part ETag by re-reading form source
                                .supplyAsync(new VerifySourcePartTask(partNumber, offset, length, existingMpuParts.get(partNumber).getRawETag()), executorService)
                                // then, if the part is invalid (throws PartMismatchException), re-upload it (if configured to do so)
                                .exceptionally(partMismatchHandler(resumeContext.getUploadId(), partNumber, offset, length)));
                    } else {
                        // calling code has specified *not* to verify existing parts found in the target, so we will
                        // trust the existing part ETag
                        log.debug("verifyPartsFoundInTarget is false; not verifying existing part data for partNumber {} (ETag: {})",
                                partNumber, existingMpuParts.get(partNumber).getETag());
                        resumeContext.getUploadedParts().put(partNumber, new MultipartPartETag(partNumber, existingMpuParts.get(partNumber).getETag()));
                    }

                    // no existing part to use, so upload this part
                } else {
                    futures.add(executorService.submit(new UploadPartTask(resumeContext.getUploadId(), partNumber, offset, length)));
                }
            }

            // wait for threads to finish and gather parts
            for (Future<MultipartPartETag> future : futures) {
                try {
                    resumeContext.getUploadedParts().put(future.get().getPartNumber(), future.get());
                } catch (ExecutionException e) { // unfortunately, we can't just catch CancellationException here
                    // CancellationException is only thrown when we are terminated early - cancelled tasks will just be ignored
                    if (e.getCause() == null || !(e.getCause() instanceof CancellationException)) throw e;
                }
            }

            // complete MP upload
            if (active.get()) {
                CompleteMultipartUploadResult result = completeMpu(resumeContext.getUploadId(), new TreeSet<>(resumeContext.getUploadedParts().values()));
                eTag = result.getRawETag();
                versionId = result.getVersionId();
            }

        } catch (Exception e) {
            // abort MP upload
            // TODO: are there conditions where the upload should *not* be aborted?
            try {
                if (abortMpuOnFailure) {
                    abortMpu(resumeContext.getUploadId());
                    resumeContext.setUploadId(null);
                    resumeContext.setUploadedParts(null);
                }
            } catch (Throwable t) {
                log.warn("could not abort upload after failure", t);
            }
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("error during upload", e);
        } finally {
            active.set(false);

            // make sure all spawned threads are shut down
            if (!externalExecutorService) executorService.shutdownNow();

            // make sure we close the input stream if necessary
            if (stream != null && closeStream) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    log.warn("could not close stream", t);
                }
            }
        }
    }

    private Function<Throwable, ? extends MultipartPartETag> partMismatchHandler(String uploadId, int partNumber, long offset, long length) {
        return throwable -> {
            // peel off the execution exception
            if (throwable instanceof CompletionException) throwable = throwable.getCause();
            if (resumeContext.isOverwriteMismatchedParts() && throwable instanceof PartMismatchException) {
                log.warn(throwable.getMessage()); // log details about the part that was mismatched
                log.info("overwriting partNumber {} due to ETag mismatch", partNumber);
                return new UploadPartTask(uploadId, partNumber, offset, length).call();
            } else if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else throw new RuntimeException(throwable);
        };
    }

    public void doByteRangeUpload() {
        configure();

        // create empty object (sets metadata/acl)
        PutObjectRequest request = new PutObjectRequest(bucket, key, null);
        request.setObjectMetadata(objectMetadata);
        request.setAcl(acl);
        request.setCannedAcl(cannedAcl);
        s3Client.putObject(request);

        List<Future<String>> futures = new ArrayList<>();
        try {
            // submit all upload tasks
            long offset = 0, length = partSize;
            while (offset < fullSize) {
                if (offset + length > fullSize) length = fullSize - offset;

                futures.add(executorService.submit(new PutObjectTask(offset, length)));

                offset += length;
            }

            // wait for threads to finish
            for (Future<String> future : futures) {
                eTag = future.get();
            }
        } catch (Exception e) {

            // delete object
            try {
                s3Client.deleteObject(bucket, key);
            } catch (Throwable t) {
                log.warn("could not delete object after failure", t);
            }
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("error during upload", e);
        } finally {
            // make sure all spawned threads are shut down
            if (!externalExecutorService) executorService.shutdown();

            // make sure we close the input stream if necessary
            if (stream != null && closeStream) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    log.warn("could not close stream", t);
                }
            }
        }
    }

    /**
     * This method should be idempotent
     */
    protected void configure() {
        // sanity checks
        if (multipartSource != null) {
            fullSize = multipartSource.getTotalSize();
        } else if (stream != null) {

            // make sure size is set
            if (fullSize <= 0)
                throw new IllegalArgumentException("size must be specified for stream");

            // If resuming from raw stream, make sure skipped parts are consumed from source stream
            if (resumeContext != null) resumeContext.setVerifyPartsFoundInTarget(true);

            // must read stream sequentially
            executorService = null;
            threads = 1;
        } else {
            throw new IllegalArgumentException("must specify a file, stream, or multipartSource to read");
        }

        // make sure content-length isn't set
        if (objectMetadata != null) objectMetadata.setContentLength(null);

        long minPartSize = Math.max(getMinPartSize(), fullSize / MAX_PARTS + 1);
        log.debug(String.format("minimum part size calculated as %,dk", minPartSize / 1024));

        if (partSize == null) partSize = minPartSize;
        if (partSize < minPartSize) {
            log.warn(String.format("%,dk is below the minimum part size (%,dk). the minimum will be used instead",
                    partSize / 1024, minPartSize / 1024));
            partSize = minPartSize;
        }

        if (resumeContext != null) {
            // we can only resume an MPU if the size of the source is above the MPU threshold
            if (fullSize < mpuThreshold) {
                throw new UnsupportedOperationException("cannot resume MPU because the size of the source is below the MPU threshold");
            }

            // calling code must provide an uploadId to resume
            if (resumeContext.getUploadId() == null) {
                throw new IllegalArgumentException("must provide an uploadId to resume");
            }
        }

        // set up thread pool
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(threads);
        } else {
            externalExecutorService = true;
        }
    }

    public S3Client getS3Client() {
        return s3Client;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public InputStream getStream() {
        return stream;
    }

    public LargeFileMultipartSource getMultipartSource() {
        return multipartSource;
    }

    public long getFullSize() {
        return fullSize;
    }

    public long getBytesTransferred() {
        return bytesTransferred.get();
    }

    /**
     * Returns the result ETag after an upload is successfully complete.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Returns the result versionId after an upload is successfully completed to a version-enabled bucket.
     */
    public String getVersionId() {
        return versionId;
    }

    public S3ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(S3ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public boolean isCloseStream() {
        return closeStream;
    }

    public void setCloseStream(boolean closeStream) {
        this.closeStream = closeStream;
    }

    public long getMpuThreshold() {
        return mpuThreshold;
    }

    /**
     * Sets the threshold above which an MPU operation is used to upload, and below which a single-PUT is used. This
     * only applies when using the {@link #upload()} method. Note the default threshold is
     * {@link #DEFAULT_MPU_THRESHOLD}
     */
    public void setMpuThreshold(long mpuThreshold) {
        this.mpuThreshold = mpuThreshold;
    }

    public long getPartSize() {
        return partSize;
    }

    /**
     * Sets the size of each part to upload. Note the default part size is {@link #DEFAULT_PART_SIZE} and
     * {@link #MIN_PART_SIZE} is the minimum part size. Note also there is a maximum of 10,000 parts, and the part size
     * will be increased automatically if necessary.
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public int getThreads() {
        return threads;
    }

    /**
     * Sets the number of threads to use for transferring parts. <code>thread</code> parts will be
     * transferred in parallel. Default is 6
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Allows for providing a custom thread executor (i.e. for custom thread factories). Note that if
     * you set a custom executor service, the <code>threads</code> property will be ignored.
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * During an upload operation, the <code>resumeContext</code> is kept up-to-date with the uploadId and list of
     * uploaded parts.
     */
    public LargeFileUploaderResumeContext getResumeContext() {
        return resumeContext;
    }

    /**
     * Use when resuming an existing incomplete MPU by skipping existing parts.
     * To resume an MPU, you *must* provide an uploadId to resume.
     * If a part list is not provided here, the uploadId parts will be listed to find existing parts to skip.
     * All parts in the existing upload must conform to the expected part size and count, based on
     * {@link #setMpuThreshold(long)} and {@link #setPartSize(long)}.
     *
     * @throws S3Exception                   if the provided uploadId does not exist, or any other S3 errors occur
     * @throws IllegalArgumentException      if the uploadId is null or any of the parts are invalid
     * @throws UnsupportedOperationException if the size of the source is *not* above <code>mpuThreshold</code>
     * @see LargeFileUploaderResumeContext
     */
    public void setResumeContext(LargeFileUploaderResumeContext resumeContext) {
        this.resumeContext = resumeContext;
    }

    public boolean isAbortMpuOnFailure() {
        return abortMpuOnFailure;
    }

    /**
     * Specifies whether MPU is aborted with any failure
     * If a failure occurs and abortMpuOnFailure is true, then MPU is aborted and the resumeContext is cleared
     * (uploadId and uploadedParts are set to null).
     * If abortMpuOnFailure is false, MPU is left intact and the resumeContext could have a list successfully
     * uploaded parts.
     */
    public void setAbortMpuOnFailure(boolean abortMpuOnFailure) {
        this.abortMpuOnFailure = abortMpuOnFailure;
    }

    public LargeFileUploader withObjectMetadata(S3ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public LargeFileUploader withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public LargeFileUploader withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }

    public LargeFileUploader withCloseStream(boolean closeStream) {
        setCloseStream(closeStream);
        return this;
    }

    public LargeFileUploader withMpuThreshold(long mpuThreshold) {
        setMpuThreshold(mpuThreshold);
        return this;
    }

    public LargeFileUploader withPartSize(Long partSize) {
        setPartSize(partSize);
        return this;
    }

    public LargeFileUploader withThreads(int threads) {
        setThreads(threads);
        return this;
    }

    public LargeFileUploader withExecutorService(ExecutorService executorService) {
        setExecutorService(executorService);
        return this;
    }

    public LargeFileUploader withProgressListener(ProgressListener progressListener) {
        setProgressListener(progressListener);
        return this;
    }

    /**
     * @see #setResumeContext(LargeFileUploaderResumeContext)
     */
    public LargeFileUploader withResumeContext(LargeFileUploaderResumeContext resumeContext) {
        setResumeContext(resumeContext);
        return this;
    }

    /**
     * @see #setAbortMpuOnFailure(boolean)
     */
    public LargeFileUploader withAbortMpuOnFailure(boolean abortMpuOnFailure) {
        setAbortMpuOnFailure(abortMpuOnFailure);
        return this;
    }

    private class UploadPartTask implements Callable<MultipartPartETag> {
        private final String uploadId;
        private final int partNumber;
        private final long offset;
        private final long length;

        public UploadPartTask(String uploadId, int partNumber, long offset, long length) {
            this.uploadId = uploadId;
            this.partNumber = partNumber;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public MultipartPartETag call() {
            if (!active.get()) {
                // we were paused or aborted, so should not start any more tasks
                throw new CancellationException();
            } else {
                log.debug("uploading {}/{}, uploadId: {}, partNumber {} (offset: {}, length: {})",
                        bucket, key, uploadId, partNumber, offset, length);
                try (InputStream is = monitorStream(getSourcePartDataStream(offset, length))) {
                    return uploadPart(uploadId, partNumber, is, length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected class PutObjectTask implements Callable<String> {
        private final long offset;
        private final long length;

        public PutObjectTask(long offset, long length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public String call() {
            try (InputStream is = monitorStream(getSourcePartDataStream(offset, length))) {
                Range range = Range.fromOffsetLength(offset, length);

                PutObjectRequest request = new PutObjectRequest(bucket, key, is).withRange(range);

                return s3Client.putObject(request).getETag();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected class VerifySourcePartTask implements Supplier<MultipartPartETag> {
        private final int partNumber;
        private final long offset, length;
        private final String uploadedETag;

        public VerifySourcePartTask(int partNumber, long offset, long length, String uploadedETag) {
            this.partNumber = partNumber;
            this.offset = offset;
            this.length = length;
            this.uploadedETag = uploadedETag;
        }

        @Override
        public MultipartPartETag get() {
            if (!active.get()) {
                // we were paused or aborted, so should not start any more tasks
                throw new CancellationException();
            } else {
                log.debug("reading existing partNumber {} (offset: {}, length: {}) from source to verify data", partNumber, offset, length);
                try (InputStream is = getSourcePartDataStream(offset, length)) {
                    String sourceETag = DigestUtils.md5Hex(is);
                    if (!sourceETag.equals(uploadedETag)) {
                        throw new PartMismatchException(partNumber, sourceETag, uploadedETag);
                    }
                    return new MultipartPartETag(partNumber, sourceETag);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
