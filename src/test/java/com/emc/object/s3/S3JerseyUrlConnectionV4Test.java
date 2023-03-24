package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PutObjectRequest;
import com.emc.util.RandomInputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;

public class S3JerseyUrlConnectionV4Test extends S3JerseyClientV4Test {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-url-connection-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        System.setProperty("http.maxConnections", "100");
        S3Config config = createS3Config().withUseV2Signer(false);
        String proxy = config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        if (proxy != null) {
            URI proxyUri = new URI(proxy);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }
        return new S3JerseyClient(config);
    }

    @Ignore // only run this test against a co-located ECS!
    @Test
    public void testVeryLargeWrite() throws Exception {
        String key = "very-large-object";
        long size = (long) Integer.MAX_VALUE + 102400;
        InputStream content = new RandomInputStream(size);
        S3ObjectMetadata metadata = new S3ObjectMetadata().withContentLength(size);
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(metadata);
        client.putObject(request);

        Assert.assertEquals(size, client.getObjectMetadata(getTestBucket(), key).getContentLength().longValue());
    }

    @Ignore // only run this test against a co-located ECS!
    @Test
    public void testVeryLargeChunkedWrite() throws Exception {
        String key = "very-large-chunked-object";
        long size = (long) Integer.MAX_VALUE + 102400;
        InputStream content = new RandomInputStream(size);
        client.putObject(getTestBucket(), key, content, null);

        Assert.assertEquals(size, client.getObjectMetadata(getTestBucket(), key).getContentLength().longValue());
    }
}
