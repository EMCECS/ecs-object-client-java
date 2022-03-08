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

import com.emc.object.s3.bean.MultipartPartETag;
import com.emc.object.s3.bean.Upload;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.lfu.LargeFileMultipartSource;
import com.emc.object.s3.lfu.LargeFileUploaderResumeContext;
import com.emc.object.s3.request.AbortMultipartUploadRequest;
import com.emc.object.s3.request.ListMultipartUploadsRequest;
import com.emc.object.s3.request.UploadPartRequest;
import com.emc.object.util.ProgressListener;
import com.emc.rest.util.StreamUtil;
import com.emc.util.RandomInputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
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

    @Override
    protected void cleanUpBucket(String bucketName) {
        // clean up MPUs
        if (client != null && client.bucketExists(bucketName)) {
            for (Upload upload : client.listMultipartUploads(bucketName).getUploads()) {
                client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, upload.getKey(), upload.getUploadId()));
            }
        }
        super.cleanUpBucket(bucketName);
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
    public void testLargeFileUploader() throws Exception {
        String key = "large-file-uploader.bin";
        int size = 20 * 1024 * 1024 + 123; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        File file = File.createTempFile("large-file-uploader-test", null);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();

        LargeFileUploader uploader = new LargeFileUploader(client, getTestBucket(), key, file);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);

        // multipart
        uploader.doMultipartUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));

        client.deleteObject(getTestBucket(), key);

        // parallel byte-range (also test metadata)
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().addUserMetadata("key", "value");
        uploader = new LargeFileUploader(client, getTestBucket(), key, file);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);
        uploader.setObjectMetadata(objectMetadata);
        uploader.doByteRangeUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));
        Assert.assertEquals(objectMetadata.getUserMetadata(), client.getObjectMetadata(getTestBucket(), key).getUserMetadata());

        // test issue 1 (https://github.com/emcvipr/ecs-object-client-java/issues/1)
        objectMetadata = new S3ObjectMetadata();
        objectMetadata.withContentLength(size);
        uploader = new LargeFileUploader(client, getTestBucket(), key + ".2", file);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);
        uploader.setObjectMetadata(objectMetadata);
        uploader.doByteRangeUpload();
    }

    @Test
    public void testLargeFileUploaderProgressListener() throws Exception {
        String key = "large-file-uploader.bin";
        int size = 20 * 1024 * 1024 + 123; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        File file = File.createTempFile("large-file-uploader-test", null);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();
        final AtomicLong completed = new AtomicLong();
        final AtomicLong total = new AtomicLong();
        final AtomicLong transferred = new AtomicLong();
        ProgressListener pl = new ProgressListener() {

            @Override
            public void progress(long c, long t) {
                completed.set(c);
                total.set(t);
            }

            @Override
            public void transferred(long size) {
                transferred.addAndGet(size);
            }
        };

        LargeFileUploader uploader = new LargeFileUploader(client, getTestBucket(), key, file).withProgressListener(pl);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);

        // multipart
        uploader.doMultipartUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));
        Assert.assertEquals(size, completed.get());
        Assert.assertEquals(size, total.get());
        Assert.assertTrue(String.format("Should transfer at least %d bytes but only got %d", size, transferred.get()),
                transferred.get() >= size);

        client.deleteObject(getTestBucket(), key);
    }

    @Test
    public void testLargeFileUploaderStream() {
        String key = "large-file-uploader-stream.bin";
        int size = 20 * 1024 * 1024 + 123; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);

        LargeFileUploader uploader = new LargeFileUploader(client, getTestBucket(), key,
                new ByteArrayInputStream(data), size);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);

        // multipart
        uploader.doMultipartUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));

        client.deleteObject(getTestBucket(), key);

        // parallel byte-range (also test metadata)
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().addUserMetadata("key", "value");
        uploader = new LargeFileUploader(client, getTestBucket(), key, new ByteArrayInputStream(data), size);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);
        uploader.setObjectMetadata(objectMetadata);
        uploader.doByteRangeUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));
        Assert.assertEquals(objectMetadata.getUserMetadata(), client.getObjectMetadata(getTestBucket(), key).getUserMetadata());
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

    @Test
    public void testResumeMpu() {
        String bucket = getTestBucket();
        String key = "myprefix/mpu-abort-test";
        long partSize = LargeFileUploader.MIN_PART_SIZE;
        long size = 4 * partSize + 1066;
        byte[] data = new byte[(int) size];
        new Random().nextBytes(data);

        // init MPU
        Date lastModifiedTime = new Date(System.currentTimeMillis());
        String uploadId = client.initiateMultipartUpload(bucket, key);
        int partNum = 1;
        for (long offset = 0; offset < data.length; offset += partSize) {
            // skip some parts: first, middle, last
            if (partNum == 1 || partNum == 3 || partNum == 5) {
                partNum++;
                continue;
            }
            long length = data.length - offset;
            if (length > partSize) length = partSize;
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum++,
                    Arrays.copyOfRange(data, (int) offset, (int) (offset + length)));
            client.uploadPart(request);
        }

        try {
            client.getObjectMetadata(bucket, key);
            Assert.fail("Object should not exist because MPU upload is incomplete");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
        }

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withResumeIfInitiatedAfter(lastModifiedTime);
        LargeFileUploader lfu = new LargeFileUploader(client, bucket, key, new ByteArrayInputStream(data), size)
                .withPartSize(partSize).withMpuThreshold(size).withResumeContext(resumeContext);
        lfu.doMultipartUpload();

        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucket).withPrefix(key);
        // will resume from previous multipart upload thus uploadId will not exist after CompleteMultipartUpload.
        Assert.assertEquals(0, client.listMultipartUploads(request).getUploads().size());
        // object is uploaded successfully
        Assert.assertEquals(size, (long) client.getObjectMetadata(bucket, key).getContentLength());
    }

    @Test
    public void testResumeMpuAfterUpdate() {
        String bucket = getTestBucket();
        String key = "myprefix/mpu-abort-test";
        long partSize = LargeFileUploader.MIN_PART_SIZE;
        long size = 4 * partSize + 1066;
        byte[] data = new byte[(int) size];
        new Random().nextBytes(data);

        // init MPU
        String uploadId = client.initiateMultipartUpload(bucket, key);
        int partNum = 1;
        for (long offset = 0; offset < data.length; offset += partSize) {
            // skip some parts: first, middle, last
            if (partNum == 1 || partNum == 3 || partNum == 5) {
                partNum++;
                continue;
            }
            long length = data.length - offset;
            if (length > partSize) length = partSize;
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum++,
                    Arrays.copyOfRange(data, (int) offset, (int) (offset + length)));
            client.uploadPart(request);
        }

        try {
            client.getObjectMetadata(bucket, key);
            Assert.fail("Object should not exist because MPU upload is incomplete");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
        }

        // generate lastModified time to simulate object updates during multipart upload.
        Date lastModifiedTime = new Date(System.currentTimeMillis());
        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withResumeIfInitiatedAfter(lastModifiedTime);
        LargeFileUploader lfu = new LargeFileUploader(client, bucket, key, new ByteArrayInputStream(data), size)
                .withPartSize(partSize).withMpuThreshold(size).withResumeContext(resumeContext);
        lfu.doMultipartUpload();

        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucket).withPrefix(key);
        List<Upload> uploads = client.listMultipartUploads(request).getUploads();
        // multipart upload will not resume from previous uploadId because of resumeSafeGuard check.
        Assert.assertEquals(uploadId, uploads.get(0).getUploadId());
        // object is uploaded successfully
        Assert.assertEquals(size, (long) client.getObjectMetadata(bucket, key).getContentLength());

        AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucket, key, uploadId);
        client.abortMultipartUpload(abortMultipartUploadRequest);
    }

    @Test
    public void testLargeFileMultiPartSource() {
        // TODO: implement
        // create LFU
        // pass in MockMultipartSource
        // verify bytes of target object
        // verify ETag (use getMpuETag())
    }

    static class NullStream extends OutputStream {
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

    public static final String[] MPS_PARTS = new String[]{
            "Hello part 1!",
            "I am part 2",
            "this is part 3",
            "mumbo jumbo a8df675as87f65d5&^%*&^%*&^%&*^%*^&\n",
            "бвгджзклмнп"
    };

    static class MockMultipartSource implements LargeFileMultipartSource {
        @Override
        public long getTotalSize() {
            return 0;
        }

        @Override
        public InputStream getCompleteDataStream() {
            return new ByteArrayInputStream(getTotalBytes());
        }

        @Override
        public InputStream getPartDataStream(long offset, long length) {
            return new ByteArrayInputStream(Arrays.copyOfRange(getTotalBytes(), (int) offset, (int) length));
        }

        byte[] getTotalBytes() {
            return String.join("", MPS_PARTS).getBytes(StandardCharsets.UTF_8);
        }

        public String getMpuETag() {
            List<MultipartPartETag> partETags = new ArrayList<>();
            for (int i = 0; i < MPS_PARTS.length; i++) {
                partETags.add(new MultipartPartETag(i + 1, DigestUtils.md5Hex(MPS_PARTS[i].getBytes(StandardCharsets.UTF_8))));
            }
            return LargeFileUploader.getMpuETag(partETags);
        }
    }
}
