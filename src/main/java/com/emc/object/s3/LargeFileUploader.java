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
import com.emc.object.s3.lfu.LargeFileMultipartFileSource;
import com.emc.object.s3.lfu.LargeFileMultipartSource;
import com.emc.object.s3.lfu.LargeFileUploaderResumeContext;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Convenience class to facilitate multipart upload for large files. This class will split the file
 * and upload it in parts, transferring several parts simultaneously to maximize efficiency.
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

    private LargeFileUploaderResumeContext resumeContext;

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

        // track progress
        is = new ProgressInputStream(is, this);

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

        // track progress
        is = new ProgressInputStream(is, this);

        return is;
    }

    public void doSinglePut() {
        configure();

        try (InputStream is = getSourceCompleteDataStream()) {
            PutObjectRequest putRequest = new PutObjectRequest(bucket, key, is);
            putRequest.setObjectMetadata(objectMetadata);
            putRequest.setAcl(acl);
            putRequest.setCannedAcl(cannedAcl);

            PutObjectResult result = s3Client.putObject(putRequest);

            eTag = result.getETag();
        } catch (IOException e) {
            throw new RuntimeException("Error opening file", e);
        }
    }

    /*
     * returns the latest (most recently initiated) MPU for the configured bucket/key that was initiated after
     * resumeIfInitiatedAfter (if set), or null if none is found.
     */
    private String getLatestMultipartUploadId() {
        Upload latestUpload = null;
        try {
            ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucket).withPrefix(key);
            ListMultipartUploadsResult result = null;
            do {
                if (result == null) {
                    result = s3Client.listMultipartUploads(request);
                } else {
                    result = s3Client.listMultipartUploads(request.withKeyMarker(result.getNextKeyMarker()).withUploadIdMarker(result.getNextUploadIdMarker()));
                }
                for (Upload upload : result.getUploads()) {
                    // filter out non-matching keys
                    if (!upload.getKey().equals(key)) continue;
                    // filter out stale uploads
                    if (resumeContext.getResumeIfInitiatedAfter() != null
                            && upload.getInitiated().before(resumeContext.getResumeIfInitiatedAfter())) {
                        log.debug("Stale Upload detected ({}): Initiated time {} shouldn't be earlier than {}.",
                                upload.getUploadId(), upload.getInitiated(), resumeContext.getResumeIfInitiatedAfter());
                        continue;
                    }
                    if (latestUpload != null) {
                        if (upload.getInitiated().after(latestUpload.getInitiated())) {
                            log.debug("found newer matching upload ({} : {})", upload.getUploadId(), upload.getInitiated());
                            latestUpload = upload;
                        } else {
                            log.debug("Skipping upload ({} : {}) because a newer one was found", upload.getUploadId(), upload.getInitiated());
                        }
                    } else {
                        log.debug("found matching upload ({} : {})", upload.getUploadId(), upload.getInitiated());
                        latestUpload = upload;
                    }
                }
            } while (result.isTruncated());
        } catch (S3Exception e) {
            log.warn("Error in retrieving MPU uploads", e);
        }

        if (latestUpload == null) return null;
        return latestUpload.getUploadId();
    }

    /*
     * get a map of exising MPU parts from which we can resume an MPU. we can only resume an MPU if the existing
     * part sizes and count are exactly the same as configured in this LFU instance
     */
    private Map<Integer, MultipartPartETag> getUploadPartsForResume(String uploadId) {
        List<MultipartPart> existingParts = s3Client.listParts(new ListPartsRequest(bucket, key, uploadId)).getParts();
        Map<Integer, MultipartPartETag> partsForResume = new HashMap<>();

        if (existingParts == null) {
            return null;
        } else {
            // sort parts based on partNumber
            existingParts.sort(Comparator.comparingInt(MultipartPartETag::getPartNumber));

            // check the parts - if any part size doesn't match, or there are more parts than expected, we cannot resume
            int lastPart = (int) ((fullSize - 1) / partSize) + 1;
            long lastPartSize = fullSize - ((lastPart - 1) * partSize);
            for (MultipartPart part : existingParts) {
                if (part.getPartNumber() > lastPart) {
                    log.debug("Too many parts in uploadId: {}: last part is {}, but saw partNumber {}",
                            uploadId, lastPart, part.getPartNumber());
                    return null; // invalid upload
                }
                long expectedSize = part.getPartNumber() == lastPart ? lastPartSize : partSize;
                if (!part.getSize().equals(expectedSize)) {
                    log.debug("Invalid part size detected in uploadId: {}/{}: expected {}, but saw {}",
                            uploadId, part.getPartNumber(), expectedSize, part.getSize());
                    return null; // invalid upload
                }

                // we can skip this part
                partsForResume.put(part.getPartNumber(), part);
            }
            return partsForResume;
        }
    }

    public void doMultipartUpload() {
        configure();

        String uploadId = null;
        Map<Integer, MultipartPartETag> partsToSkip = null;

        if (resumeContext != null) {
            // these should be set in configure()
            uploadId = resumeContext.getUploadId();
            partsToSkip = resumeContext.getPartsToSkip();
        }

        if (uploadId == null) {
            // initiate MP upload
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, key);
            initRequest.setObjectMetadata(objectMetadata);
            initRequest.setAcl(acl);
            initRequest.setCannedAcl(cannedAcl);
            uploadId = s3Client.initiateMultipartUpload(initRequest).getUploadId();
        }

        List<Future<MultipartPartETag>> futures = new ArrayList<>();
        SortedSet<MultipartPartETag> parts = new TreeSet<>();
        try {
            // submit all upload tasks
            int lastPart = (int) ((fullSize - 1) / partSize) + 1;
            for (int partNumber = 1; partNumber <= lastPart; partNumber++) {
                long offset = (partNumber - 1) * partSize;
                long length = partSize;
                if (offset + length > fullSize) length = fullSize - offset;

                // skip upload and reuse existing parts if found.
                if (partsToSkip != null && partsToSkip.containsKey(partNumber)) {
                    log.info("bucket {} key {} partNumber {} already exists, will be reused for multipart upload", bucket, key, partNumber);
                    // re-read source part if necessary
                    if (resumeContext.isVerifySkippedParts()) {
                        futures.add(executorService.submit(new ReadSourcePartTask(partNumber, offset, length)));
                    } else {
                        parts.add(new MultipartPartETag(partNumber, partsToSkip.get(partNumber).getETag()));
                    }
                } else {
                    futures.add(executorService.submit(new UploadPartTask(uploadId, partNumber, offset, length)));
                }
            }

            // TODO: allow calling code to stop the upload, allowing in-transfer parts to complete, but then shut down
            //       and return completed part ETags (ResumeContext)
            // wait for threads to finish and gather parts
            for (Future<MultipartPartETag> future : futures) {
                parts.add(future.get());
            }

            // complete MP upload
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucket, key, uploadId).withParts(parts);
            CompleteMultipartUploadResult result = s3Client.completeMultipartUpload(compRequest);
            eTag = result.getETag();

        } catch (Exception e) {
            // abort MP upload
            try {
                s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
            } catch (Throwable t) {
                log.warn("could not abort upload after failure", t);
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
            if (resumeContext != null) resumeContext.setVerifySkippedParts(true);

            // must read stream sequentially
            executorService = null;
            threads = 1;
        } else {
            throw new IllegalArgumentException("must specify a file, stream, or multipartSource to read");
        }

        // make sure content-length isn't set
        if (objectMetadata != null) objectMetadata.setContentLength(null);

        long minPartSize = Math.max(MIN_PART_SIZE, fullSize / MAX_PARTS + 1);
        log.debug(String.format("minimum part size calculated as %,dk", minPartSize / 1024));

        if (partSize == null) partSize = minPartSize;
        if (partSize < minPartSize) {
            log.warn(String.format("%,dk is below the minimum part size (%,dk). the minimum will be used instead",
                    partSize / 1024, minPartSize / 1024));
            partSize = minPartSize;
        }

        // if we are resuming an MPU, size is above MPU threshold, and we don't have an uploadId to resume,
        // find an existing upload to resume
        if (resumeContext != null && fullSize >= mpuThreshold) {
            // find latest upload
            if (resumeContext.getUploadId() == null) {
                resumeContext.setUploadId(getLatestMultipartUploadId());
            }
            // list existing parts
            if (resumeContext.getUploadId() != null && resumeContext.getPartsToSkip() == null) {
                resumeContext.setPartsToSkip(getUploadPartsForResume(resumeContext.getUploadId()));
                if (resumeContext.getPartsToSkip() == null) {
                    log.info("Latest uploadID {} is unsafe to be resumed, will start new multipart upload.", resumeContext.getUploadId());
                    resumeContext.setUploadId(null);
                }
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

    public String getETag() {
        return eTag;
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

    public LargeFileUploaderResumeContext getResumeContext() {
        return resumeContext;
    }

    /**
     * Setting this property enables resuming incomplete MPU uploads.
     * This means an upload is not aborted on failure, and if existing parts
     * are found, the upload is resumed by skipping any existing parts.
     *
     * @see LargeFileUploaderResumeContext
     */
    public void setResumeContext(LargeFileUploaderResumeContext resumeContext) {
        this.resumeContext = resumeContext;
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
     * Setting this property enables resuming incomplete MPU uploads.
     * This means an upload is not aborted on failure, and if existing parts
     * are found, the upload is resumed by skipping any existing parts.
     *
     * @see LargeFileUploaderResumeContext
     */
    public LargeFileUploader withResumeContext(LargeFileUploaderResumeContext resumeContext) {
        setResumeContext(resumeContext);
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
        public MultipartPartETag call() throws Exception {
            try (InputStream is = getSourcePartDataStream(offset, length)) {
                UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNumber, is);
                request.setContentLength(length);

                return s3Client.uploadPart(request);
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
        public String call() throws Exception {
            try (InputStream is = getSourcePartDataStream(offset, length)) {
                Range range = Range.fromOffsetLength(offset, length);

                PutObjectRequest request = new PutObjectRequest(bucket, key, is).withRange(range);

                return s3Client.putObject(request).getETag();
            }
        }
    }

    protected class ReadSourcePartTask implements Callable<MultipartPartETag> {
        private final int partNumber;
        private final long offset, length;

        public ReadSourcePartTask(int partNumber, long offset, long length) {
            this.partNumber = partNumber;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public MultipartPartETag call() throws Exception {
            String sourceETag = DigestUtils.md5Hex(getSourcePartDataStream(offset, length));
            return new MultipartPartETag(partNumber, sourceETag);
        }
    }
}
