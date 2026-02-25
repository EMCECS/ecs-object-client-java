package com.emc.object.s3;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import com.emc.object.s3.jersey.S3JerseyClient;

public class Sdk238V4Test extends Sdk238Test{
    @Override
    @Test
    public void testTrailingSlash() throws Exception {
        S3Config s3Config = AbstractS3ClientTest.s3ConfigFromProperties();
        Sdk238V4Test.TestClient testClient = new Sdk238V4Test.TestClient(s3Config);

        String bucket = "test-trailing-slash-v4";
        testClient.createBucket(bucket);
        try {
            if (s3Config.isUseVHost()) {
                Assert.assertEquals("/", testClient.getLastUri().getPath());
            } else {
                Assert.assertEquals("/" + bucket, testClient.getLastUri().getPath());
            }
        } finally {
            testClient.deleteBucket(bucket);
        }
    }

    private class TestClient extends S3JerseyClient {
        private final UriCaptureFilter captureFilter = new UriCaptureFilter();

        TestClient(S3Config s3Config) {
            super(s3Config.withUseV2Signer(false));
            client.register(captureFilter);
        }

        URI getLastUri() {
            return captureFilter.getLastUri();
        }
    }
}
