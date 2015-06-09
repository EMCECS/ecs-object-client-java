/*
 * Copyright (c) 2015, EMC Corporation.
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
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.s3.bean.MultipartPartETag;
import com.emc.object.s3.request.*;
import com.emc.object.util.InputStreamSegment;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Convenience class to facilitate multipart upload for large files. This class will split the file
 * and upload it in parts, transferring several parts simultaneously to maximize efficiency.
 */
public class LargeFileUploader implements Runnable {
    private static final Logger l4j = Logger.getLogger(LargeFileUploader.class);

    public static final int DEFAULT_THREADS = 8;

    public static final long MIN_PART_SIZE = 5 * 1024 * 1024; // 5MB
    public static final int MAX_PARTS = 10000;

    private S3Client s3Client;
    private String bucket;
    private String key;
    private S3ObjectMetadata objectMetadata;
    private AccessControlList acl;
    private CannedAcl cannedAcl;
    private File file;
    private Long partSize;
    private int threads = DEFAULT_THREADS;
    private ExecutorService executorService;

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

    @Override
    public void run() {
        doMultipartUpload();
    }

    public void doMultipartUpload() {

        // sanity checks
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("cannot read file: " + file.getPath());

        long minPartSize = Math.max(MIN_PART_SIZE, file.length() / MAX_PARTS + 1);
        l4j.info(String.format("minimum part size calculated as %,dk", minPartSize / 1024));

        if (partSize == null) partSize = minPartSize;
        if (partSize < minPartSize) {
            l4j.warn(String.format("%,dk is below the minimum part size (%,dk). the minimum will be used instead",
                    partSize / 1024, minPartSize / 1024));
            partSize = minPartSize;
        }

        // set up thread pool
        if (executorService == null) executorService = Executors.newFixedThreadPool(threads);
        List<Future<MultipartPartETag>> futures = new ArrayList<Future<MultipartPartETag>>();

        // initiate MP upload
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, key);
        request.setObjectMetadata(objectMetadata);
        request.setAcl(acl);
        request.setCannedAcl(cannedAcl);
        String uploadId = s3Client.initiateMultipartUpload(request).getUploadId();

        try {
            // submit all upload tasks
            UploadFilePartRequest partRequest;
            int partNumber = 1;
            long offset = 0, length = partSize;
            while (offset < file.length()) {
                if (offset + length > file.length()) length = file.length() - offset;
                partRequest = new UploadFilePartRequest(bucket, key, uploadId, partNumber++);
                partRequest.withFile(file).withOffset(offset).withLength(length);
                futures.add(executorService.submit(new UploadPartTask(partRequest)));
                offset += length;
            }

            // wait for threads to finish and gather parts
            SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
            for (Future<MultipartPartETag> future : futures) {
                parts.add(future.get());
            }

            // complete MP upload
            s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucket, key, uploadId).withParts(parts));

        } catch (Exception e) {

            // abort MP upload
            s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucket, key, uploadId));
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("error uploading file", e);
        } finally {

            // make sure all spawned threads are shut down
            executorService.shutdown();
        }
    }

    public void doByteRangeUpload() {

        // sanity checks
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("cannot read file: " + file.getPath());

        long minPartSize = Math.max(MIN_PART_SIZE, file.length() / MAX_PARTS + 1);
        l4j.info(String.format("minimum part size calculated as %,dk", minPartSize / 1024));

        if (partSize == null) partSize = minPartSize;
        if (partSize < minPartSize) {
            l4j.warn(String.format("%,dk is below the minimum part size (%,dk). the minimum will be used instead",
                    partSize / 1024, minPartSize / 1024));
            partSize = minPartSize;
        }

        // set up thread pool
        if (executorService == null) executorService = Executors.newFixedThreadPool(threads);
        List<Future> futures = new ArrayList<Future>();

        // create empty object (sets metadata/acl)
        PutObjectRequest request = new PutObjectRequest(bucket, key, null);
        request.setObjectMetadata(objectMetadata);
        request.setAcl(acl);
        request.setCannedAcl(cannedAcl);
        s3Client.putObject(request);

        try {
            // submit all upload tasks
            PutObjectRequest rangeRequest;
            long offset = 0, length = partSize;
            while (offset < file.length()) {
                if (offset + length > file.length()) length = file.length() - offset;
                Range range = Range.fromOffsetLength(offset, length);
                InputStreamSegment segment = new InputStreamSegment(new FileInputStream(file), offset, length);
                rangeRequest = new PutObjectRequest(bucket, key, segment).withRange(range);
                futures.add(executorService.submit(new PutObjectTask(rangeRequest)));
                offset += length;
            }

            // wait for threads to finish
            for (Future future : futures) {
                future.get();
            }
        } catch (Exception e) {

            // delete object
            s3Client.deleteObject(bucket, key);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("error uploading file", e);
        } finally {

            // make sure all spawned threads are shut down
            executorService.shutdown();
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

    public File getFile() {
        return file;
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

    public long getPartSize() {
        return partSize;
    }

    /**
     * Sets the size of each part to upload. This should not be necessary as the part size will be
     * automatically calculated based on the size of the file. Note that 5MB is the minimum part size and
     * there is a maximum of 10,000 parts.
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

    protected class UploadPartTask implements Callable<MultipartPartETag> {
        private UploadPartRequest request;

        public UploadPartTask(UploadPartRequest request) {
            this.request = request;
        }

        @Override
        public MultipartPartETag call() throws Exception {
            return s3Client.uploadPart(request);
        }
    }

    protected class PutObjectTask implements Runnable {
        private PutObjectRequest request;

        public PutObjectTask(PutObjectRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            s3Client.putObject(request);
        }
    }
}
