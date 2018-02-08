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

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;

public class LargeFileUploaderTest extends AbstractS3ClientTest {
    static final int FILE_SIZE = 20 * 1024 * 1024; // 20MB

    File tempFile;
    String md5Hex;

    @Override
    protected String getTestBucketPrefix() {
        return "lfu-test";
    }

    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    @Before
    public void createTempFile() throws Exception {
        tempFile = File.createTempFile("lfu-test", null);
        tempFile.deleteOnExit();

        DigestInputStream dis = new DigestInputStream(new RandomInputStream(FILE_SIZE), MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new FileOutputStream(tempFile), FILE_SIZE);
        md5Hex = DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase();
    }

    @Test
    public void testAboveThreshold() throws Exception {
        String key = "lfu-mpu-test";
        long partSize = FILE_SIZE / 5;
        final AtomicLong bytesTransferred = new AtomicLong(), bytesCompleted = new AtomicLong(), bytesTotal = new AtomicLong();

        // upload in 4MB parts
        LargeFileUploader lfu = new LargeFileUploader(client, getTestBucket(), key, tempFile);
        lfu.withMpuThreshold(FILE_SIZE).withPartSize(partSize);
        lfu.setProgressListener(new ProgressListener() {
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
        lfu.upload();

        // verify MPU
        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(FILE_SIZE, metadata.getContentLength().longValue());
        Assert.assertTrue(metadata.getETag().endsWith("-" + FILE_SIZE / partSize));

        // verify progress indicators
        Assert.assertEquals(FILE_SIZE, lfu.getBytesTransferred());
        Assert.assertEquals(FILE_SIZE, bytesCompleted.get());
        Assert.assertEquals(FILE_SIZE, bytesTotal.get());
        Assert.assertEquals(FILE_SIZE, bytesTransferred.get());

        // verify content
        DigestInputStream dis = new DigestInputStream(client.readObjectStream(getTestBucket(), key, null),
                MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new NullStream(), metadata.getContentLength());
        Assert.assertEquals(md5Hex, DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase());
    }

    @Test
    public void testBelowThreshold() throws Exception {
        String key = "lfu-single-test";
        final AtomicLong bytesTransferred = new AtomicLong(), bytesCompleted = new AtomicLong(), bytesTotal = new AtomicLong();

        // upload in single stream
        LargeFileUploader lfu = new LargeFileUploader(client, getTestBucket(), key, tempFile);
        lfu.withMpuThreshold(FILE_SIZE + 1);
        lfu.setProgressListener(new ProgressListener() {
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
        lfu.upload();

        // verify no MPU
        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(FILE_SIZE, metadata.getContentLength().longValue());
        Assert.assertEquals(md5Hex, metadata.getETag().toLowerCase());

        // verify progress indicators
        Assert.assertEquals(FILE_SIZE, lfu.getBytesTransferred());
        Assert.assertEquals(FILE_SIZE, bytesCompleted.get());
        Assert.assertEquals(FILE_SIZE, bytesTotal.get());
        Assert.assertEquals(FILE_SIZE, bytesTransferred.get());

        // verify content
        DigestInputStream dis = new DigestInputStream(client.readObjectStream(getTestBucket(), key, null),
                MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new NullStream(), metadata.getContentLength());
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
