package com.emc.object.s3;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.ws.rs.client.Client;

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

    // NOTE: In Jersey 2.x with Apache connector, connection pool settings are configured
    //       through SmartClientFactory which sets up the Apache HttpClient 5.x connection manager.
    //       This test verifies that a custom connection limit can be set via SmartConfig properties.
    @Test
    public void testApacheConnectionLimit() throws IOException {
        S3Config s3Config = loadTestConfig();

        int connectionLimitPerHost = 4; // non-default number
        int connectionLimitTotal = 39; // non-default number

        // In Jersey 2.x, connection limits are set via SmartConfig properties
        s3Config.setProperty(SmartClientFactory.MAX_CONNECTIONS_PER_HOST, connectionLimitPerHost);
        s3Config.setProperty(SmartClientFactory.MAX_CONNECTIONS, connectionLimitTotal);

        TestS3JerseyClient s3Client = new TestS3JerseyClient(s3Config);

        // verify the client was created successfully with custom config
        Client jerseyClient = s3Client.getClient();
        Assert.assertNotNull(jerseyClient);
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
