/*
 * Copyright (c) 2015-2018, EMC Corporation.
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

import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.util.ProgressListener;
import com.emc.rest.util.StreamUtil;
import com.emc.util.RandomInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jakarta.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class LargeFileDownloaderTest extends AbstractS3ClientTest {
    static final long FILE_SIZE = 20 * 1024 * 1024; // 20MB

    DigestInputStream dis;
    File sourceFile;
    File destFile;
    String md5Hex;
    String key;

    @Override
    protected String getTestBucketPrefix() {
        return "lfd-test";
    }

    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    @Before
    public void createObject() throws Exception {
        sourceFile = File.createTempFile("lfd-source", null);
        sourceFile.deleteOnExit();
        destFile = File.createTempFile("lfd-dest", null);
        destFile.deleteOnExit();

        // create temp file
        dis = new DigestInputStream(new RandomInputStream(FILE_SIZE), MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new FileOutputStream(sourceFile), FILE_SIZE);
        md5Hex = DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase();

        // upload using LFU (this is done to save time)
        key = "lfd-test";
        LargeFileUploader lfu = new LargeFileUploader(client, getTestBucket(), key, sourceFile).withPartSize(FILE_SIZE / 5);
        lfu.doMultipartUpload();
    }

    @Test
    public void testLargeFileDownloader() throws Exception {
        String key = "large-file-downloader.bin";
        int size = 20 * 1024 * 1024 + 179; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        client.putObject(getTestBucket(), key, data, null);

        File file = File.createTempFile("large-file-uploader-test", null);
        file.deleteOnExit();
        LargeFileDownloader downloader = new LargeFileDownloader(client, getTestBucket(), key, file);
        downloader.run();

        byte[] readData = new byte[size];
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.read(readData);
        raf.close();

        Assert.assertArrayEquals(data, readData);
    }

    @Test
    public void testAboveThreshold() throws Exception {
        long partSize = FILE_SIZE / 5;
        final AtomicLong bytesTransferred = new AtomicLong(), bytesCompleted = new AtomicLong(), bytesTotal = new AtomicLong();

        // downlaod in 4MB chunks
        LargeFileDownloader lfd = new LargeFileDownloader(client, getTestBucket(), key, destFile);
        lfd.withParallelThreshold(FILE_SIZE).withPartSize(partSize);
        lfd.setProgressListener(new ProgressListener() {
            @Override
            public void progress(long completed, long total) {
                bytesCompleted.set(completed);
                bytesTotal.set(total);
            }

            @Override
            public void transferred(long size) {
                bytesTransferred.addAndGet(size);
            }
        });
        lfd.download();

        // verify threads were used (this will be null if single-GET)
        Assert.assertNotNull(lfd.getExecutorService());

        // verify progress indicators
        Assert.assertEquals(FILE_SIZE, lfd.getBytesTransferred());
        Assert.assertEquals(FILE_SIZE, bytesCompleted.get());
        Assert.assertEquals(FILE_SIZE, bytesTotal.get());
        Assert.assertEquals(FILE_SIZE, bytesTransferred.get());

        // verify content
        DigestInputStream dis = new DigestInputStream(new FileInputStream(destFile), MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new NullStream(), destFile.length());
        Assert.assertEquals(md5Hex, DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase());
    }

    @Test
    public void testBelowThreshold() throws Exception {
        final AtomicLong bytesTransferred = new AtomicLong(), bytesCompleted = new AtomicLong(), bytesTotal = new AtomicLong();

        // download in single stream
        LargeFileDownloader lfd = new LargeFileDownloader(client, getTestBucket(), key, destFile);
        lfd.withParallelThreshold(FILE_SIZE + 1);
        lfd.setProgressListener(new ProgressListener() {
            @Override
            public void progress(long completed, long total) {
                bytesCompleted.set(completed);
                bytesTotal.set(total);
            }

            @Override
            public void transferred(long size) {
                bytesTransferred.addAndGet(size);
            }
        });
        lfd.download();

        // verify single thread (thread pool should be null)
        Assert.assertNull(lfd.getExecutorService());

        // verify progress indicators
        Assert.assertEquals(FILE_SIZE, lfd.getBytesTransferred());
        Assert.assertEquals(FILE_SIZE, bytesCompleted.get());
        Assert.assertEquals(FILE_SIZE, bytesTotal.get());
        Assert.assertEquals(FILE_SIZE, bytesTransferred.get());

        // verify content
        DigestInputStream dis = new DigestInputStream(new FileInputStream(destFile), MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new NullStream(), destFile.length());
        Assert.assertEquals(md5Hex, DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase());
    }

    class NullStream extends OutputStream {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }
}
