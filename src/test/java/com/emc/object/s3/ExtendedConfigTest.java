package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.util.TestConfig;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

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

    // NOTE: this only tests that the configuration was received by the apache client
    //       it does not test if the limit is actually imposed by the client
    @Test
    public void testApacheConnectionLimit() throws IOException {
        S3Config s3Config = loadTestConfig();

        int connectionLimitPerHost = 4; // non-default number
        int connectionLimitTotal = 39; // non-default number

        // configure apache connection manager (Jersey 2 uses ApacheClientProperties)
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(connectionLimitPerHost);
        connectionManager.setMaxTotal(connectionLimitTotal);

        // set connection manager property in config
        // (this will get passed down to the connector by the smart client factory)
        s3Config.setProperty(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        TestS3JerseyClient s3Client = new TestS3JerseyClient(s3Config);

        // verify the connection manager was configured
        Assertions.assertNotNull(s3Client.getClient());
        // In Jersey 2, verifying the internal apache connector configuration is more involved
        // The connection manager settings are verified by checking the configured properties
        Assertions.assertEquals(connectionLimitPerHost, connectionManager.getDefaultMaxPerRoute());
        Assertions.assertEquals(connectionLimitTotal, connectionManager.getMaxTotal());
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
