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

import com.emc.object.s3.bean.GetObjectResult;
import com.emc.object.s3.bean.MultipartPart;
import com.emc.object.s3.bean.MultipartPartETag;
import com.emc.object.s3.bean.Upload;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.lfu.LargeFileMultipartSource;
import com.emc.object.s3.lfu.LargeFileUpload;
import com.emc.object.s3.lfu.LargeFileUploaderResumeContext;
import com.emc.object.s3.lfu.PartMismatchException;
import com.emc.object.s3.request.AbortMultipartUploadRequest;
import com.emc.object.s3.request.GetObjectRequest;
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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class LargeFileUploaderTest extends AbstractS3ClientTest {
    static final int FILE_SIZE = 5 * 1024 * 1024; // 5MB

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

        LargeFileUploader uploader = new TestLargeFileUploader(client, getTestBucket(), key, file);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);

        // multipart
        uploader.doMultipartUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));

        client.deleteObject(getTestBucket(), key);

        // parallel byte-range (also test metadata)
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().addUserMetadata("key", "value");
        uploader = new TestLargeFileUploader(client, getTestBucket(), key, file);
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
        uploader = new TestLargeFileUploader(client, getTestBucket(), key + ".2", file);
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
        ByteProgressListener pl = new ByteProgressListener();

        LargeFileUploader uploader = new TestLargeFileUploader(client, getTestBucket(), key, file).withProgressListener(pl);
        uploader.setPartSize(LargeFileUploader.MIN_PART_SIZE);

        // multipart
        uploader.doMultipartUpload();

        Assert.assertEquals(size, uploader.getBytesTransferred());
        Assert.assertTrue(uploader.getETag().contains("-")); // hyphen signifies multipart / updated object
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));
        Assert.assertEquals(size, pl.completed.get());
        Assert.assertEquals(size, pl.total.get());
        Assert.assertTrue(String.format("Should transfer at least %d bytes but only got %d", size, pl.transferred.get()),
                pl.transferred.get() >= size);

        client.deleteObject(getTestBucket(), key);
    }

    @Test
    public void testLargeFileUploaderStream() {
        String key = "large-file-uploader-stream.bin";
        int size = 20 * 1024 * 1024 + 123; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);

        LargeFileUploader uploader = new TestLargeFileUploader(client, getTestBucket(), key,
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
        uploader = new TestLargeFileUploader(client, getTestBucket(), key, new ByteArrayInputStream(data), size);
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

        // upload in 4MB parts
        LargeFileUploader lfu = new TestLargeFileUploader(client, getTestBucket(), key, tempFile);
        lfu.withMpuThreshold(FILE_SIZE).withPartSize(partSize);
        ByteProgressListener pl = new ByteProgressListener();
        lfu.setProgressListener(pl);
        lfu.upload();

        // verify MPU
        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(FILE_SIZE, metadata.getContentLength().longValue());
        Assert.assertTrue(metadata.getETag().endsWith("-" + FILE_SIZE / partSize));

        // verify progress indicators
        Assert.assertEquals(FILE_SIZE, lfu.getBytesTransferred());
        Assert.assertEquals(FILE_SIZE, pl.completed.get());
        Assert.assertEquals(FILE_SIZE, pl.total.get());
        Assert.assertEquals(FILE_SIZE, pl.transferred.get());

        // verify content
        DigestInputStream dis = new DigestInputStream(client.readObjectStream(getTestBucket(), key, null),
                MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new NullStream(), metadata.getContentLength());
        Assert.assertEquals(md5Hex, DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase());
    }

    @Test
    public void testBelowThreshold() throws Exception {
        String key = "lfu-single-test";

        // upload in single stream
        LargeFileUploader lfu = new TestLargeFileUploader(client, getTestBucket(), key, tempFile);
        lfu.withMpuThreshold(FILE_SIZE + 1);
        ByteProgressListener pl = new ByteProgressListener();
        lfu.setProgressListener(pl);
        lfu.upload();

        // verify no MPU
        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(FILE_SIZE, metadata.getContentLength().longValue());
        Assert.assertEquals(md5Hex, metadata.getETag().toLowerCase());

        // verify progress indicators
        Assert.assertEquals(FILE_SIZE, lfu.getBytesTransferred());
        Assert.assertEquals(FILE_SIZE, pl.completed.get());
        Assert.assertEquals(FILE_SIZE, pl.total.get());
        Assert.assertEquals(FILE_SIZE, pl.transferred.get());

        // verify content
        DigestInputStream dis = new DigestInputStream(client.readObjectStream(getTestBucket(), key, null),
                MessageDigest.getInstance("MD5"));
        StreamUtil.copy(dis, new NullStream(), metadata.getContentLength());
        Assert.assertEquals(md5Hex, DatatypeConverter.printHexBinary(dis.getMessageDigest().digest()).toLowerCase());
    }

    @Test
    public void testResumeMpuFromStream() {
        String bucket = getTestBucket();
        String key = "myprefix/mpu-resume-test-stream";
        final long partSize = 500 * 1024; // 500 KiB
        final long size = 4 * partSize + 1066;
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

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withUploadId(uploadId);
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, new ByteArrayInputStream(data), size)
                .withPartSize(partSize).withMpuThreshold(size).withResumeContext(resumeContext);
        lfu.doMultipartUpload();

        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucket).withPrefix(key);
        // will resume from previous multipart upload thus uploadId will not exist after CompleteMultipartUpload.
        Assert.assertEquals(0, client.listMultipartUploads(request).getUploads().size());
        // object is uploaded successfully
        Assert.assertEquals(size, (long) client.getObjectMetadata(bucket, key).getContentLength());
    }

    @Test
    public void testResumeMpuFromMultiPartSource() {
        String bucket = getTestBucket();
        String key = "myprefix/mpu-resume-test-mps";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        final long partSize = mockMultipartSource.getPartSize();

        // generate last modified time to be 5s ahead of upload for better tolerance of slight time drift
        Date lastModifiedTime = new Date(System.currentTimeMillis() - 5000);
        // init MPU
        String uploadId = client.initiateMultipartUpload(bucket, key);

        // upload first 2 parts
        for (int partNum = 1; partNum <= 2; partNum++) {
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum,
                    mockMultipartSource.getPartDataStream((partNum - 1) * partSize, partSize));
            MultipartPartETag multipartPartETag = client.uploadPart(request);
        }

        try {
            client.getObjectMetadata(bucket, key);
            Assert.fail("Object should not exist because MPU upload is incomplete");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
        }

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withUploadId(uploadId);
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize()).withResumeContext(resumeContext);
        lfu.doMultipartUpload();

        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucket).withPrefix(key);
        // will resume from previous multipart upload thus uploadId will not exist after CompleteMultipartUpload.
        Assert.assertEquals(0, client.listMultipartUploads(request).getUploads().size());
        // object is uploaded successfully
        S3ObjectMetadata om = client.getObjectMetadata(bucket, key);
        Assert.assertEquals(mockMultipartSource.getTotalSize(), (long)om.getContentLength());
        Assert.assertEquals(mockMultipartSource.getMpuETag(), om.getETag());
    }

    @Test
    public void testLargeFileMultiPartSource() {
        String key = "testLargeFileMultiPartSource";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        LargeFileUploader lfu = new TestLargeFileUploader(client, getTestBucket(), key, mockMultipartSource)
                .withPartSize(mockMultipartSource.getPartSize()).withMpuThreshold((int) mockMultipartSource.getTotalSize());
        lfu.doMultipartUpload();

        GetObjectRequest request = new GetObjectRequest(getTestBucket(), key);
        GetObjectResult<byte[]> result = client.getObject(request, byte[].class);
        Assert.assertArrayEquals(mockMultipartSource.getTotalBytes(), result.getObject());
        Assert.assertEquals(mockMultipartSource.getMpuETag(), client.getObjectMetadata(getTestBucket(), key).getETag());
    }

    @Test
    public void testResumeWithPartList() {
        String bucket = getTestBucket();
        String key = "mpu-resume-with-partlist";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        final long partSize = mockMultipartSource.getPartSize();
        int totalPartsToResume = 2;
        Map<Integer, MultipartPartETag> uploadedParts = new HashMap<>();

        ByteProgressListener pl = new ByteProgressListener();

        // init MPU
        String uploadId = client.initiateMultipartUpload(bucket, key);

        // upload first 2 parts
        for (int partNum = 1; partNum <= totalPartsToResume; partNum++) {
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum,
                    mockMultipartSource.getPartDataStream((partNum - 1) * partSize, partSize));
            MultipartPartETag multipartPartETag = client.uploadPart(request);
            uploadedParts.put(partNum, multipartPartETag);
        }

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withUploadId(uploadId)
                .withUploadedParts(uploadedParts);
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize()).withResumeContext(resumeContext)
                .withProgressListener(pl);
        lfu.doMultipartUpload();

        S3ObjectMetadata om = client.getObjectMetadata(bucket, key);
        Assert.assertEquals(mockMultipartSource.getTotalSize(), (long) om.getContentLength());
        Assert.assertEquals(mockMultipartSource.getMpuETag(), om.getETag());

        Assert.assertEquals(mockMultipartSource.getTotalSize() - partSize * totalPartsToResume, lfu.getBytesTransferred());
        Assert.assertEquals(mockMultipartSource.getTotalSize() - partSize * totalPartsToResume, pl.completed.get());
        Assert.assertEquals(mockMultipartSource.getTotalSize(), pl.total.get());
    }


    @Test
    public void testResumeWithPartListAndBadPart() {
        String bucket = getTestBucket();
        String key = "mpu-resume-with-bad-part-etag";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        final long partSize = mockMultipartSource.getPartSize();
        int totalPartsToResume = 2;
        Map<Integer, MultipartPartETag> uploadedParts = new HashMap<>();

        // init MPU
        String uploadId = client.initiateMultipartUpload(bucket, key);

        // upload first 2 parts
        for (int partNum = 1; partNum <= totalPartsToResume; partNum++) {
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum,
                    mockMultipartSource.getPartDataStream((partNum - 1) * partSize, partSize));
            MultipartPartETag multipartPartETag = client.uploadPart(request);
            uploadedParts.put(partNum, multipartPartETag);
        }

        // crate a wrong MultipartPartETag by reversing Etag
        MultipartPartETag badMultipartPartETag = new MultipartPartETag(1, new StringBuilder(uploadedParts.get(1).getETag()).reverse().toString());
        uploadedParts.replace(1, badMultipartPartETag);

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withUploadId(uploadId)
                .withUploadedParts(uploadedParts);
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize()).withResumeContext(resumeContext);
        try {
            lfu.doMultipartUpload();
            Assert.fail("one of the ETags in uploadedParts is wrong - should abort the upload and throw an exception");
        } catch (S3Exception e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("InvalidPart", e.getErrorCode());
        }
        try {
            client.listParts(bucket, key, uploadId);
            Assert.fail("UploadId should not exist because MPU is aborted");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchUpload", e.getErrorCode());
        }
    }

    @Test
    public void testResumeWithBadPartOverwrite() {
        testResumeWithBadPart(true);
    }

    @Test
    public void testResumeWithBadPartNoOverwrite() {
        testResumeWithBadPart(false);
    }

    private void testResumeWithBadPart(boolean overwriteBadPart) {
        String bucket = getTestBucket();
        String key = "mpu-resume-with-bad-part-data";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        final long partSize = mockMultipartSource.getPartSize();
        int totalPartsToResume = 2;

        ByteProgressListener pl = new ByteProgressListener();

        // init MPU
        String uploadId = client.initiateMultipartUpload(bucket, key);

        // upload first 2 parts
        for (int partNum = 1; partNum <= totalPartsToResume; partNum++) {
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum,
                    mockMultipartSource.getPartDataStream((partNum - 1) * partSize, partSize));
            client.uploadPart(request);
            if (partNum == totalPartsToResume) {
                // simulate an uploaded part got wrong data
                byte[] data = new byte[(int) partSize];
                new Random().nextBytes(data);
                request = new UploadPartRequest(bucket, key, uploadId, partNum, new ByteArrayInputStream(data));
                client.uploadPart(request);
            }
        }

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withUploadId(uploadId);
        resumeContext.setOverwriteMismatchedParts(overwriteBadPart);
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource).withPartSize(partSize)
                .withMpuThreshold(mockMultipartSource.getTotalSize()).withResumeContext(resumeContext)
                .withProgressListener(pl);
        try {
            lfu.doMultipartUpload();
            if (!overwriteBadPart)
                Assert.fail("one of the data in uploadedParts is wrong - should abort the upload and throw an exception");
        } catch (RuntimeException e) {
            if (overwriteBadPart) throw e;
            // root exception will be wrapped in ExecutionException and then RuntimeException
            Assert.assertNotNull(e.getCause());
            Assert.assertNotNull(e.getCause().getCause());
            Assert.assertTrue(e.getCause().getCause() instanceof PartMismatchException);
            // make sure the failed part was expected
            Assert.assertEquals(totalPartsToResume, ((PartMismatchException) e.getCause().getCause()).getPartNumber());
        }
        if (overwriteBadPart) {
            // should have re-uploaded the bad part, so this should be reflected in the bytes transferred
            Assert.assertEquals(mockMultipartSource.getTotalSize() - (partSize * (totalPartsToResume - 1)), lfu.getBytesTransferred());
            Assert.assertEquals(mockMultipartSource.getTotalSize() - (partSize * (totalPartsToResume - 1)), pl.completed.get());
            Assert.assertEquals(mockMultipartSource.getTotalSize(), pl.total.get());
            Assert.assertEquals(mockMultipartSource.getMpuETag(), lfu.getETag());
        } else {
            // MPU should be aborted because of a bad part
            try {
                client.listParts(bucket, key, uploadId);
                Assert.fail("UploadId should not exist because MPU is aborted");
            } catch (S3Exception e) {
                Assert.assertEquals(404, e.getHttpCode());
                Assert.assertEquals("NoSuchUpload", e.getErrorCode());
            }
        }
    }

    @Test
    public void testResumeWithBadPartAndNoVerify() {
        String bucket = getTestBucket();
        String key = "mpu-resume-with-bad-part-data-no-verify";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        final long partSize = mockMultipartSource.getPartSize();
        int totalPartsToResume = 2;

        ByteProgressListener pl = new ByteProgressListener();

        // init MPU
        String uploadId = client.initiateMultipartUpload(bucket, key);

        // upload first 2 parts
        for (int partNum = 1; partNum <= totalPartsToResume; partNum++) {
            UploadPartRequest request = new UploadPartRequest(bucket, key, uploadId, partNum,
                    mockMultipartSource.getPartDataStream((partNum - 1) * partSize, partSize));
            client.uploadPart(request);
            if (partNum == totalPartsToResume) {
                // simulate an uploaded part got wrong data
                byte[] data = new byte[(int)partSize];
                new Random().nextBytes(data);
                request = new UploadPartRequest(bucket, key, uploadId, partNum, new ByteArrayInputStream(data));
                client.uploadPart(request);
            }
        }

        LargeFileUploaderResumeContext resumeContext = new LargeFileUploaderResumeContext().withUploadId(uploadId)
                .withVerifyPartsFoundInTarget(false);
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize())
                .withResumeContext(resumeContext).withProgressListener(pl);
        lfu.doMultipartUpload();

        S3ObjectMetadata om = client.getObjectMetadata(bucket, key);
        Assert.assertEquals(mockMultipartSource.getTotalSize(), (long) om.getContentLength());
        Assert.assertNotEquals(mockMultipartSource.getMpuETag(), om.getETag());

        Assert.assertEquals(mockMultipartSource.getTotalSize() - partSize * totalPartsToResume, lfu.getBytesTransferred());
        Assert.assertEquals(mockMultipartSource.getTotalSize() - partSize * totalPartsToResume, pl.completed.get());
        Assert.assertEquals(mockMultipartSource.getTotalSize(), pl.total.get());
    }

    @Test
    public void testPauseResume() throws Exception {
        String bucket = getTestBucket();
        String key = "mpu-pause";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        // 1-second delay before yielding part streams
        // this allows us to time the start of the first 2 part uploads accurately
        mockMultipartSource.setPartDelayMs(2000);
        final long partSize = mockMultipartSource.getPartSize();

        ByteProgressListener pl = new ByteProgressListener();

        // set 2 threads and pause immediately - only 2 parts should be uploaded
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize())
                .withThreads(2).withProgressListener(pl);
        LargeFileUpload upload = lfu.uploadAsync();

        // wait for first 2 parts to start streaming
        Thread.sleep(2000);

        LargeFileUploaderResumeContext resumeContext = upload.pause();

        // object should not exist
        try {
            Assert.assertNull(client.getObjectMetadata(bucket, key));
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchKey", e.getErrorCode());
        }

        // check resume context accuracy
        Assert.assertNotNull(resumeContext.getUploadId());
        Assert.assertNotNull(resumeContext.getUploadedParts());
        Assert.assertEquals(2, resumeContext.getUploadedParts().size());

        // check only the bytes of 2 parts were xferred
        Assert.assertEquals(2 * partSize, pl.transferred.get());
        Assert.assertEquals(2 * partSize, pl.completed.get());

        // make sure only 2 parts were uploaded and the ETags match our list
        List<MultipartPart> parts = client.listParts(getTestBucket(), key, resumeContext.getUploadId()).getParts();
        Assert.assertNotNull(parts);
        Assert.assertEquals(2, parts.size());
        Assert.assertEquals(parts.get(0).getRawETag(), resumeContext.getUploadedParts().get(1).getRawETag());
        Assert.assertEquals(parts.get(1).getRawETag(), resumeContext.getUploadedParts().get(2).getRawETag());

        // disable delay in part streams
        mockMultipartSource.setPartDelayMs(0);
        pl = new ByteProgressListener();
        lfu = new TestLargeFileUploader(client, getTestBucket(), key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize())
                .withProgressListener(pl).withResumeContext(resumeContext);

        lfu.doMultipartUpload();

        // check complete object
        Assert.assertEquals(mockMultipartSource.getMpuETag(), client.getObjectMetadata(getTestBucket(), key).getETag());

        // check only remaining parts were uploaded
        Assert.assertEquals(mockMultipartSource.getTotalSize() - (2 * partSize), pl.transferred.get());
        Assert.assertEquals(mockMultipartSource.getTotalSize() - (2 * partSize), pl.completed.get());
    }

    @Test
    public void testAsyncWithTimeout() {
        String key = "testLfuAsyncTimeout";
        int delayMs = 2000;
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        // 1-second delay before yielding part streams
        // this allows us to time the start of the first 2 part uploads accurately
        mockMultipartSource.setPartDelayMs(delayMs);
        LargeFileUploader lfu = new TestLargeFileUploader(client, getTestBucket(), key, mockMultipartSource)
                .withPartSize(mockMultipartSource.getPartSize()).withMpuThreshold((int) mockMultipartSource.getTotalSize())
                .withThreads(2); // throttle part uploads to 2 threads

        LargeFileUpload upload = lfu.uploadAsync();

        int timeoutCount = 0;
        while (true) {
            try {
                upload.waitForCompletion(delayMs, TimeUnit.MILLISECONDS);
                break;
            } catch (TimeoutException e) {
                timeoutCount++;
            }
        }

        // based on part count, thread count and part delay, we expect (ceiling(partCount / threadCount)) timeouts to occur
        long partCount = (mockMultipartSource.getTotalSize() - 1) / mockMultipartSource.getPartSize() + 1;
        Assert.assertEquals((partCount - 1) / 2 + 1, timeoutCount);

        // upload should be done
        GetObjectRequest<?> request = new GetObjectRequest<>(getTestBucket(), key);
        GetObjectResult<byte[]> result = client.getObject(request, byte[].class);
        Assert.assertArrayEquals(mockMultipartSource.getTotalBytes(), result.getObject());
        Assert.assertEquals(mockMultipartSource.getMpuETag(), result.getObjectMetadata().getETag());
    }

    @Test
    public void testAbort() throws Exception {
        String bucket = getTestBucket();
        String key = "mpu-abort";
        MockMultipartSource mockMultipartSource = new MockMultipartSource();
        // 1-second delay before yielding part streams
        // this allows us to time the start of the first 2 part uploads accurately
        mockMultipartSource.setPartDelayMs(2000);
        final long partSize = mockMultipartSource.getPartSize();

        // set 2 threads and pause immediately - only 2 parts should be uploaded
        LargeFileUploader lfu = new TestLargeFileUploader(client, bucket, key, mockMultipartSource)
                .withPartSize(partSize).withMpuThreshold(mockMultipartSource.getTotalSize())
                .withThreads(2);
        LargeFileUpload upload = lfu.uploadAsync();

        // wait for first 4 parts to start streaming
        Thread.sleep(3000);

        // make sure the upload was started
        Assert.assertEquals(1, client.listMultipartUploads(getTestBucket()).getUploads().size());

        // abort it
        long abortStart = System.currentTimeMillis();
        upload.abort();
        long abortDone = System.currentTimeMillis();

        // this should be immediate (< 500ms)
        Assert.assertTrue(abortDone - abortStart < 500);

        // make sure resume context is cleared
        Assert.assertNull(lfu.getResumeContext().getUploadId());
        Assert.assertNull(lfu.getResumeContext().getUploadedParts());

        // object should not exist
        try {
            Assert.assertNull(client.getObjectMetadata(bucket, key));
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchKey", e.getErrorCode());
        }

        // upload should not exist
        Assert.assertEquals(0, client.listMultipartUploads(getTestBucket()).getUploads().size());
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

    static class MockMultipartSource implements LargeFileMultipartSource {
        private static byte[] MPS_PARTS;
        private static final long partSize = 500 * 1024; // 500 KiB
        private static final long totalSize = partSize * 4 + 123; // 5 parts
        private long partDelayMs = 0;

        public MockMultipartSource() {
            synchronized (MockMultipartSource.class) {
                if (MPS_PARTS == null) {
                    MPS_PARTS = new byte[(int) totalSize];
                    new Random().nextBytes(MPS_PARTS);
                }
            }
        }

        @Override
        public long getTotalSize() { return totalSize; }

        @Override
        public InputStream getCompleteDataStream() {
            return new ByteArrayInputStream(getTotalBytes());
        }

        @Override
        public InputStream getPartDataStream(long offset, long length) {
            if (partDelayMs > 0) try {
                Thread.sleep(partDelayMs);
            } catch (InterruptedException ignored) {
            }
            return new ByteArrayInputStream(Arrays.copyOfRange(getTotalBytes(), (int) offset, (int) (offset + length)));
        }

        byte[] getTotalBytes() { return MPS_PARTS; }

        public long getPartSize() { return partSize; }

        public String getMpuETag() {
            List<MultipartPartETag> partETags = new ArrayList<>();
            int totalParts = (int) ((totalSize - 1) / partSize + 1);
            for (int i = 0; i < totalParts; i++) {
                int from = (int) partSize * i;
                int to = (int) (from + partSize);
                partETags.add(new MultipartPartETag(i + 1, DigestUtils.md5Hex(Arrays.copyOfRange(MPS_PARTS, from, to <= getTotalSize() ? to : (int) getTotalSize()))));
            }
            return LargeFileUploader.getMpuETag(partETags);
        }

        public long getPartDelayMs() {
            return partDelayMs;
        }

        public void setPartDelayMs(long partDelayMs) {
            this.partDelayMs = partDelayMs;
        }
    }

    static class ByteProgressListener implements ProgressListener {
        final AtomicLong completed = new AtomicLong();
        final AtomicLong total = new AtomicLong();
        final AtomicLong transferred = new AtomicLong();

        @Override
        public void progress(long c, long t) {
            completed.set(c);
            total.set(t);
        }

        @Override
        public void transferred(long size) {
            transferred.addAndGet(size);
        }
    }
}
