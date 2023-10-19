package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.CreateBucketRequest;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.s3.request.UploadPartRequest;
import com.emc.object.util.FaultInjectionStream;
import com.emc.util.ConcurrentJunitRunner;
import org.apache.commons.codec.digest.DigestUtils;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.ProcessingException;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

@RunWith(ConcurrentJunitRunner.class)
public class WriteTruncationTest extends AbstractS3ClientTest {
    static final int OBJECT_RETENTION_PERIOD = 15; // 15 seconds
    static final int MOCK_OBJ_SIZE = 5 * 1024 * 1024; // 5MB

    S3Client jvmClient;
    final Random random = new Random();

    @Override
    protected S3Client createS3Client() throws Exception {
        S3Config s3Config = createS3Config().withRetryEnabled(false);
        this.jvmClient = new S3JerseyClient(s3Config, "HTTPURLCONNECTION");
        return new S3JerseyClient(createS3Config().withRetryEnabled(false));
    }

    @Override
    protected String getTestBucketPrefix() {
        return "s3-write-truncation-test";
    }

    @Override
    protected void createBucket(String bucketName) {
        // create bucket with retention period and D@RE enabled
        client.createBucket(new CreateBucketRequest(getTestBucket())
                .withRetentionPeriod(OBJECT_RETENTION_PERIOD)
                .withEncryptionEnabled(true));
    }

    @Override
    protected void cleanUpBucket(String bucketName) {
        try {
            Thread.sleep(OBJECT_RETENTION_PERIOD * 1000); // wait for retention to expire
        } catch (InterruptedException ignored) {
        }
        super.cleanUpBucket(bucketName);
    }

    @After
    public void shutdownJvmClient() {
        if (jvmClient != null) jvmClient.destroy();
    }

    @Test
    public void testIOExceptionDuringReadApache() {
        testTruncatedWrite(true, true, ExceptionType.IOException, 0, false);
    }

    @Test
    public void testIOExceptionDuringReadJvm() {
        testTruncatedWrite(false, true, ExceptionType.IOException, 0, false);
    }

    @Test
    public void testRuntimeExceptionDuringReadApache() {
        testTruncatedWrite(true, true, ExceptionType.RuntimeException, 0, false);
    }

    @Test
    public void testRuntimeExceptionDuringReadJvm() {
        testTruncatedWrite(false, true, ExceptionType.RuntimeException, 0, false);
    }

    // This will fail because ECS does not know how much data is the correct amount (as would be the case if
    // Content-Length was used)
    // TODO: the client still seems to send a 0-byte chunk terminator, which it probably shouldn't
    //       - see if we can stop the write such that ECS does not commit it
    @Ignore
    @Test
    public void testIOExceptionChunkedEncodingApache() {
        testTruncatedWrite(true, false, ExceptionType.IOException, 0, false);
    }

    // This will fail because ECS does not know how much data is the correct amount (as would be the case if
    // Content-Length was used)
    // TODO: the client still seems to send a 0-byte chunk terminator, which it probably shouldn't
    //       - see if we can stop the write such that ECS does not commit it
    @Ignore
    @Test
    public void testIOExceptionChunkedEncodingJvm() {
        testTruncatedWrite(false, false, ExceptionType.IOException, 0, false);
    }

    // (see above)
    // However, when a Content-MD5 header is sent on the PUT, ECS will reject it if the data doesn't match - this is our workaround
    @Test
    public void testIOExceptionChunkedEncodingMd5Apache() {
        testTruncatedWrite(true, false, ExceptionType.IOException, 0, true);
    }

    // (see above)
    // However, when a Content-MD5 header is sent on the PUT, ECS will reject it if the data doesn't match - this is our workaround
    @Test
    public void testIOExceptionChunkedEncodingMd5Jvm() {
        testTruncatedWrite(false, false, ExceptionType.IOException, 0, true);
    }

    @Test
    public void testDelayedIOExceptionApache() {
        testTruncatedWrite(true, true, ExceptionType.IOException, 61, false);
    }

    @Test
    public void testDelayedIOExceptionJvm() {
        testTruncatedWrite(false, true, ExceptionType.IOException, 61, false);
    }

    void testTruncatedWrite(boolean useApacheClient,
                            boolean setContentLength,
                            ExceptionType exceptionType,
                            int delayBeforeException,
                            boolean sendContentMd5) {
        S3Client s3Client = useApacheClient ? this.client : this.jvmClient;

        String key = String.format("read-%s%s-%s%stest",
                delayBeforeException > 0 ? "delayed-" : "",
                exceptionType,
                setContentLength ? "chunked-" : "",
                useApacheClient ? "apache-" : "jvm-");
        S3ObjectMetadata metadata = new S3ObjectMetadata();
        if (setContentLength) metadata.withContentLength(MOCK_OBJ_SIZE);
        metadata.withRetentionPeriod((long) OBJECT_RETENTION_PERIOD);
        String message = "Injected Exception for testing purposes";

        byte[] data = new byte[MOCK_OBJ_SIZE];
        random.nextBytes(data);
        InputStream dataStream = new ByteArrayInputStream(data);

        if (sendContentMd5) metadata.setContentMd5(DatatypeConverter.printBase64Binary(DigestUtils.md5(data)));

        FaultInjectionStream badStream;
        if (exceptionType == ExceptionType.RuntimeException) {
            badStream = new FaultInjectionStream(dataStream, MOCK_OBJ_SIZE / 2, new RuntimeException(message));
        } else {
            badStream = new FaultInjectionStream(dataStream, MOCK_OBJ_SIZE / 2, new IOException(message));
        }
        badStream.setSecondDelayBeforeThrowing(delayBeforeException);

        try {
            s3Client.putObject(new PutObjectRequest(getTestBucket(), key, badStream).withObjectMetadata(metadata));
            Assert.fail("exception in input stream did not throw an exception");
        } catch (RuntimeException e) {
            // get RC
//            Throwable t = e;
//            while (t.getCause() != null && t.getCause() != t) t = t.getCause();
            if (exceptionType == ExceptionType.RuntimeException) {
                Assert.assertTrue(e.getCause() instanceof RuntimeException);
            } else {
                Assert.assertTrue(e.getCause() instanceof IOException);
            }
            Assert.assertEquals(message, e.getCause().getMessage());
        }

        // TODO: sometimes the object is created, but does not show in a list right away - figure out why (is this a bug?)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        Assert.assertEquals(0, s3Client.listObjects(getTestBucket()).getObjects().size());
    }

    @Test
    public void testPartUploadIOException() throws Exception {
        S3Client s3Client = new S3JerseyClient(createS3Config()); // apache client with retry enabled
        try {
            String key = "mpu-part-IOException-apache-test";
            String message = "Injected Exception for testing purposes";

            byte[] data = new byte[MOCK_OBJ_SIZE];
            random.nextBytes(data);
            InputStream dataStream = new ByteArrayInputStream(data);

            FaultInjectionStream badStream;
            badStream = new FaultInjectionStream(dataStream, MOCK_OBJ_SIZE / 2, new IOException(message));

            String uploadId = s3Client.initiateMultipartUpload(getTestBucket(), key);
            try {
                s3Client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1, badStream)
                        .withContentLength((long) MOCK_OBJ_SIZE));
                Assert.fail("exception in input stream did not throw an exception");
            } catch (RuntimeException e) {
                // get RC
//                Throwable t = e;
//                while (t.getCause() != null && t.getCause() != t) t = t.getCause();
                Assert.assertTrue(e.getCause() instanceof IOException);
                Assert.assertEquals(message, e.getCause().getMessage());

                // object should not exist
                Assert.assertEquals(0, s3Client.listObjects(getTestBucket()).getObjects().size());
                // upload should exist, but should have no parts
                Assert.assertEquals(0, s3Client.listParts(getTestBucket(), key, uploadId).getParts().size());
            } finally {
                cleanMpus(getTestBucket());
            }
        } finally {
            s3Client.destroy();
        }
    }

    enum ExceptionType {
        IOException, RuntimeException
    }
}
