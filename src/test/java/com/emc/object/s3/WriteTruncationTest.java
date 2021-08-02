package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.bean.ListObjectsResult;
import com.emc.object.s3.bean.S3Object;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.CreateBucketRequest;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.util.FaultInjectionStream;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.junit.*;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Random;

public class WriteTruncationTest {
    public static final Logger log = Logger.getLogger(WriteTruncationTest.class);

    static final String BUCKET_NAME = "ecs-object-client-write-truncation-test";
    static final int OBJECT_RETENTION_PERIOD = 15; // 15 seconds
    static final int MOCK_OBJ_SIZE = 5 * 1024 * 1024; // 5MB

    S3Client s3Client;
    S3Client s3JvmClient;
    final Random random = new Random();

    @Before
    public void setup() throws Exception {
        S3Config s3Config = AbstractS3ClientTest.s3ConfigFromProperties();
        s3Config.setRetryEnabled(false);

        String proxy = s3Config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        if (proxy != null) {
            URI proxyUri = new URI(proxy);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }

        s3Client = new S3JerseyClient(s3Config);
        s3JvmClient = new S3JerseyClient(s3Config, new URLConnectionClientHandler());

        try {
            // create bucket with retention period and D@RE enabled
            s3Client.createBucket(new CreateBucketRequest(BUCKET_NAME)
                    .withRetentionPeriod(OBJECT_RETENTION_PERIOD)
                    .withEncryptionEnabled(true));
        } catch (S3Exception e) {
            if (!e.getErrorCode().equals("BucketAlreadyExists")) throw e;
        }
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
        S3Client s3Client = useApacheClient ? this.s3Client : this.s3JvmClient;

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
            s3Client.putObject(new PutObjectRequest(BUCKET_NAME, key, badStream).withObjectMetadata(metadata));
            Assert.fail("exception in input stream did not throw an exception");
        } catch (ClientHandlerException e) {
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

        Assert.assertEquals(0, s3Client.listObjects(BUCKET_NAME).getObjects().size());
    }

    @After
    public void teardown() {
        if (s3Client == null) return;

        try {
            Thread.sleep(OBJECT_RETENTION_PERIOD * 1000); // wait for retention to expire
        } catch (InterruptedException ignored) {
        }

        try {
            ListObjectsResult listing = null;
            do {
                if (listing == null) listing = s3Client.listObjects(BUCKET_NAME);
                else listing = s3Client.listMoreObjects(listing);

                for (final S3Object summary : listing.getObjects()) {
                    s3Client.deleteObject(BUCKET_NAME, summary.getKey());
                }
            } while (listing.isTruncated());

            s3Client.deleteBucket(BUCKET_NAME);
        } catch (RuntimeException e) {
            log.error("could not delete bucket " + BUCKET_NAME, e);
        } finally {
            s3Client.destroy();
        }
    }

    enum ExceptionType {
        IOException, RuntimeException
    }
}
