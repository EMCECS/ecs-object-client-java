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
import com.emc.object.s3.request.*;
import com.emc.object.util.InputStreamSegment;
import com.emc.object.util.ProgressInputStream;
import com.emc.object.util.ProgressListener;
import com.emc.rest.util.SizedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

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

    private S3Client s3Client;
    private String bucket;
    private String key;
    private File file;
    private InputStream stream;
    private long fullSize;
    private AtomicLong bytesTransferred = new AtomicLong();
    private String eTag;

    private S3ObjectMetadata objectMetadata;
    private AccessControlList acl;
    private CannedAcl cannedAcl;
    private boolean closeStream = true;
    private long mpuThreshold = DEFAULT_MPU_THRESHOLD;
    private Long partSize = DEFAULT_PART_SIZE;
    private int threads = DEFAULT_THREADS;
    private ExecutorService executorService;
    private ProgressListener progressListener;
    private boolean resumeMPU = false;
    private Upload uploadForResume;

    /**
     * Creates a new LargeFileUpload instance using the specified <code>s3Client</code> to upload
     * <code>file</code> to <code>bucket/key</code>.
     */
    public LargeFileUploader(S3Client s3Client, String bucket, String key, File file) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.file = file;
    }

    public LargeFileUploader(S3Client s3Client, String bucket, String key, InputStream stream, long size) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.stream = stream;
        this.fullSize = size;
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

    public void doSinglePut() {
        configure();

        InputStream is = null;
        try {
            is = file != null ? new FileInputStream(file) : stream;

            is = new ProgressInputStream(is, this);

            PutObjectRequest putRequest = new PutObjectRequest(bucket, key, is);
            putRequest.setObjectMetadata(objectMetadata);
            putRequest.setAcl(acl);
            putRequest.setCannedAcl(cannedAcl);

            PutObjectResult result = s3Client.putObject(putRequest);

            eTag = result.getETag();
        } catch (IOException e) {
            throw new RuntimeException("Error opening file", e);
        } finally {
            // make sure we close the input stream if necessary
            if (is != null && closeStream) {
                try {
                    is.close();
                } catch (Throwable t) {
                    log.warn("could not close stream", t);
                }
            }
        }
    }

    private Upload getLatestMultipartUpload(String bucket) {
        Upload latestUpload = null;
        try {
            ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucket).withPrefix(key);
            ListMultipartUploadsResult result = null;
            do {
                if (result == null)
                    result = s3Client.listMultipartUploads(request);
                else
                    result = s3Client.listMultipartUploads(request.withKeyMarker(result.getNextKeyMarker()).withUploadIdMarker(result.getNextUploadIdMarker()));
                for (Upload upload : result.getUploads()) {
                    if (!upload.getKey().equals(key))
                        continue;
                    if (latestUpload != null) {
                        if (upload.getInitiated().after(latestUpload.getInitiated())){
                            latestUpload = upload;
                            log.debug("Skip uploadId {} because it's initiated earlier than {}", latestUpload.getUploadId(), upload.getUploadId());
                        } else {
                            log.debug("Skip uploadId {} because it's initiated earlier than {}", upload.getUploadId(), latestUpload.getUploadId());
                        }
                    } else latestUpload = upload;
                }
            } while (result.isTruncated());
        }catch (S3Exception e) {
            log.warn("Error in retrieving MPU uploads", e);
        }
        return latestUpload;
    }


    public void doMultipartUpload() {
        configure();

        String uploadId = null;
        List<MultipartPart> mpp = null;

        if (resumeMPU && uploadForResume != null) {
            uploadId = uploadForResume.getUploadId();
            mpp = s3Client.listParts(new ListPartsRequest(bucket, key, uploadId)).getParts();
        }

        if (uploadId == null) {
            // initiate MP upload
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucket, key);
            initRequest.setObjectMetadata(objectMetadata);
            initRequest.setAcl(acl);
            initRequest.setCannedAcl(cannedAcl);
            uploadId = s3Client.initiateMultipartUpload(initRequest).getUploadId();
        }

        List<Future<MultipartPartETag>> futures = new ArrayList<Future<MultipartPartETag>>();
        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        try {
            // submit all upload tasks
            int partNumber = 1;
            long offset = 0, length = partSize;
            while (offset < fullSize) {
                boolean foundPart = false;
                if (offset + length > fullSize) length = fullSize - offset;
                if (mpp != null) {
                    for (MultipartPart part : mpp) {
                        if (part.getPartNumber() == partNumber) {
                            if (part.getSize() != length) {
                                throw new RuntimeException(String.format("Abort resume MPU due to incorrect size detected by uploadID %s, partNum %d: expect %d, actual %d", uploadId, partNumber, part.getSize(), length));
                            } else {
                                foundPart = true;
                                parts.add(new MultipartPartETag(partNumber++, part.getETag()));
                                log.debug("bucket {} key {} partNumber {} already exists, skip to resume MPU", bucket, key, partNumber);
                            }
                            break;
                        }
                    }
                }
                if (!foundPart)
                    futures.add(executorService.submit(new UploadPartTask(uploadId, partNumber++, offset, length)));

                offset += length;
            }

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
                if (!resumeMPU)
                    s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
            } catch (Throwable t) {
                log.warn("could not abort upload after failure", t);
            }
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("error during upload", e);
        } finally {
            // make sure all spawned threads are shut down
            executorService.shutdown();

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

        List<Future<String>> futures = new ArrayList<Future<String>>();
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
            executorService.shutdown();

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
        if (file != null) {
            if (!file.exists() || !file.canRead())
                throw new IllegalArgumentException("cannot read file: " + file.getPath());

            fullSize = file.length();
        } else {
            if (stream == null)
                throw new IllegalArgumentException("must specify a file or stream to read");

            // make sure size is set
            if (fullSize <= 0)
                throw new IllegalArgumentException("size must be specified for stream");

            // must read stream sequentially
            executorService = null;
            threads = 1;
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

        if (resumeMPU && fullSize >= mpuThreshold)
            uploadForResume = getLatestMultipartUpload(bucket);

        // set up thread pool
        if (executorService == null) executorService = Executors.newFixedThreadPool(threads);
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

    public File getFile() {
        return file;
    }

    public InputStream getStream() {
        return stream;
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

    public boolean isResumeMPU() {
        return resumeMPU;
    }

    public void setResumeMPU(boolean resumeMPU) {
        this.resumeMPU = resumeMPU;
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

    public LargeFileUploader withResumeMPU(boolean resumeMPU) {
        setResumeMPU(resumeMPU);
        return this;
    }

    private class UploadPartTask implements Callable<MultipartPartETag> {
        private String uploadId;
        private int partNumber;
        private long offset;
        private long length;

        public UploadPartTask(String uploadId, int partNumber, long offset, long length) {
            this.uploadId = uploadId;
            this.partNumber = partNumber;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public MultipartPartETag call() throws Exception {
            InputStream is = file != null ? new FileInputStream(file) : stream;

            is = new ProgressInputStream(is, LargeFileUploader.this);

            SizedInputStream segmentStream;
            if (file != null) {
                segmentStream = new InputStreamSegment(is, offset, length);
            } else {
                segmentStream = new SizedInputStream(is, length);
            }

            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNumber++, segmentStream);
            request.setContentLength(length);

            return s3Client.uploadPart(request);
        }
    }

    protected class PutObjectTask implements Callable<String> {
        private long offset;
        private long length;

        public PutObjectTask(long offset, long length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public String call() throws Exception {
            Range range = Range.fromOffsetLength(offset, length);

            InputStream is = file != null ? new FileInputStream(file) : stream;

            is = new ProgressInputStream(is, LargeFileUploader.this);

            SizedInputStream segmentStream = file != null
                    ? new InputStreamSegment(is, offset, length)
                    : new SizedInputStream(is, length);

            PutObjectRequest request = new PutObjectRequest(bucket, key, segmentStream).withRange(range);

            return s3Client.putObject(request).getETag();
        }
    }
}
