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
import com.emc.object.s3.request.GetObjectRequest;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Convenience class to facilitate multi-threaded download for large objects. This class will split the object
 * and download it in parts, transferring several parts simultaneously to maximize efficiency.
 */
public class LargeFileDownloader implements Runnable {
    public static final Logger l4j = Logger.getLogger(LargeFileDownloader.class);

    public static final int MIN_PART_SIZE = 1024 * 1024; // 1MB
    public static final int DEFAULT_PART_SIZE = 5 * MIN_PART_SIZE; // 5MB

    public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32KB

    public static final int DEFAULT_THREADS = 6;

    private S3Client s3Client;
    private String bucket;
    private String key;
    private File file;
    private long partSize = DEFAULT_PART_SIZE;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private int threads = DEFAULT_THREADS;
    private ExecutorService executorService;

    /**
     * Creates a new LargeFileDownloader instance that will use <code>s3Client</code> to download
     * <code>bucket/key</code> to <code>file</code>.
     */
    public LargeFileDownloader(S3Client s3Client, String bucket, String key, File file) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.key = key;
        this.file = file;
    }

    @Override
    public void run() {
        // sanity checks
        if (file.exists() && !file.canWrite())
            throw new IllegalArgumentException("cannot write to file: " + file.getPath());

        if (partSize < MIN_PART_SIZE) {
            l4j.warn(String.format("%,dk is below the minimum part size (%,dk). the minimum will be used instead",
                    partSize / 1024, MIN_PART_SIZE / 1024));
            partSize = MIN_PART_SIZE;
        }

        // get object metadata (for size)
        S3ObjectMetadata objectMetadata = s3Client.getObjectMetadata(bucket, key);
        long objectSize = objectMetadata.getContentLength();

        // set up thread pool
        if (executorService == null) executorService = Executors.newFixedThreadPool(threads);
        List<Future<Void>> futures = new ArrayList<Future<Void>>();

        try {
            // open file for random write
            RandomAccessFile raFile = new RandomAccessFile(file, "rw");
            raFile.setLength(objectSize);
            FileChannel channel = raFile.getChannel();


            // submit all download tasks
            GetObjectRequest request;
            long offset = 0, length = partSize;
            while (offset < objectSize) {
                if (offset + length > objectSize) length = objectSize - offset;
                futures.add(executorService.submit(new DownloadPartTask(Range.fromOffsetLength(offset, length), channel)));
                offset += length;
            }

            // wait for threads to finish
            for (Future<Void> future : futures) {
                future.get();
            }

            // close file
            raFile.close();

        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("error downloading file", e);
        } finally {

            // make sure all spawned threads are shut down
            executorService.shutdownNow();
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

    public long getPartSize() {
        return partSize;
    }

    /**
     * Sets the size of each part to download. Note that 1MB is the minimum part size and
     * the default is 5MB.
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets the size of the buffer to use when reading object parts. Default is 32K
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
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

    protected class DownloadPartTask implements Callable<Void> {
        private Range range;
        private FileChannel channel;

        public DownloadPartTask(Range range, FileChannel channel) {
            this.range = range;
            this.channel = channel;
        }

        @Override
        public Void call() throws Exception {
            byte[] data = s3Client.getObject(new GetObjectRequest(bucket, key).withRange(range), byte[].class).getObject();
            channel.write(ByteBuffer.wrap(data), range.getFirst());
            return null;
        }
    }
}
