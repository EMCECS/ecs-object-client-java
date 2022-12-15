package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;
import javax.ws.rs.client.ClientResponseFilter;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sdk238V4Test extends Sdk238Test{
    @Override
    @Test
    public void testTrailingSlash() throws Exception {
        S3Config s3Config = AbstractS3ClientTest.s3ConfigFromProperties();
        Sdk238V4Test.TestClient client = new Sdk238V4Test.TestClient(s3Config);

        String bucket = "test-trailing-slash-v4";
        client.createBucket(bucket);
        try {
            if (s3Config.isUseVHost()) {
                Assert.assertEquals("/", client.getLastUri().getPath());
            } else {
                Assert.assertEquals("/" + bucket, client.getLastUri().getPath());
            }
        } finally {
            client.deleteBucket(bucket);
        }
    }

    private class TestClient extends S3JerseyClient {
        private UriCaptureFilter captureFilter = new UriCaptureFilter();

        TestClient(S3Config s3Config) {
            super(s3Config.withUseV2Signer(false));

            List<ClientFilter> filters = new ArrayList<ClientFilter>();

            ClientHandler handler = client.getHeadHandler();
            while (handler instanceof ClientFilter) {
                ClientFilter filter = (ClientFilter) handler;
                filters.add(filter);
                handler = filter.getNext();
            }

            filters.add(captureFilter);

            Collections.reverse(filters);
            client.removeAllFilters();
            for (ClientFilter filter : filters) {
                client.addFilter(filter);
            }
        }

        URI getLastUri() {
            return captureFilter.getLastUri();
        }
    }
}
