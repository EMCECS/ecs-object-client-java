package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import java.net.URI;

public class S3JerseyUrlConnectionV4Test extends S3JerseyUrlConnectionTest {
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
        return new S3JerseyClient(config, new URLConnectionClientHandler());
    }
}
