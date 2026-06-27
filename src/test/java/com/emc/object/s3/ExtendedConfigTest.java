package com.emc.object.s3;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.ws.rs.client.Client;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Assert;
import org.junit.Test;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.rest.smart.jersey.SmartClientFactory;
import com.emc.util.TestConfig;

public class ExtendedConfigTest {
    private S3Config loadTestConfig() throws IOException {
        Properties props = TestConfig.getProperties();
        String accessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ACCESS_KEY);
        String secretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECRET_KEY);
        URI endpoint = URI.create(TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ENDPOINT));
        boolean enableVhost = Boolean.parseBoolean(props.getProperty(TestProperties.ENABLE_VHOST));
        boolean disableSmartClient = Boolean.parseBoolean(props.getProperty(TestProperties.DISABLE_SMART_CLIENT));
        String proxyUri = props.getProperty(TestProperties.PROXY_URI);

        S3Config s3Config;
        if (enableVhost) {
            s3Config = new S3Config(endpoint).withUseVHost(true);
        } else if (endpoint.getPort() > 0) {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), new Vdc(endpoint.getHost()));
            s3Config.setPort(endpoint.getPort());
        } else {
            s3Config = new S3Config(Protocol.valueOf(endpoint.getScheme().toUpperCase()), endpoint.getHost());
        }
        s3Config.withIdentity(accessKey).withSecretKey(secretKey);

        if (proxyUri != null) s3Config.setProperty(ObjectConfig.PROPERTY_PROXY_URI, proxyUri);

        if(disableSmartClient)
            s3Config.setSmartClient(false);

        return s3Config;
    }

    @Test
    public void testApacheConnectionLimit() throws IOException {
        S3Config s3Config = loadTestConfig();

        int connectionLimitPerHost = 4; // non-default number
        int connectionLimitTotal = 39; // non-default number

        s3Config.setProperty(SmartClientFactory.MAX_CONNECTIONS_PER_HOST, connectionLimitPerHost);
        s3Config.setProperty(SmartClientFactory.MAX_CONNECTIONS, connectionLimitTotal);

        TestS3JerseyClient s3Client = new TestS3JerseyClient(s3Config);

        // verify actual Apache connection pool settings were applied
        Client jerseyClient = s3Client.getClient();
        PoolingHttpClientConnectionManager cm = (PoolingHttpClientConnectionManager)
                jerseyClient.getConfiguration().getProperty(SmartClientFactory.CONNECTION_MANAGER_PROPERTY_KEY);
        Assert.assertNotNull("Apache connection manager not found in client config", cm);
        Assert.assertEquals(connectionLimitPerHost, cm.getDefaultMaxPerRoute());
        Assert.assertEquals(connectionLimitTotal, cm.getMaxTotal());
    }

    static class TestS3JerseyClient extends S3JerseyClient {
        public TestS3JerseyClient(S3Config s3Config) {
            super(s3Config);
        }

        Client getClient() {
            return client;
        }
    }
}
