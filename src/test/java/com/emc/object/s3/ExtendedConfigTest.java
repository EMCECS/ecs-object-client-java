package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.util.TestConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.junit.Assert;
import org.junit.Test;

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

        // configure apache connection manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(connectionLimitPerHost);
        connectionManager.setMaxTotal(connectionLimitTotal);

        // set connection manager property in config
        // (this will get passed down to the handler by the smart client factory)
        s3Config.setProperty(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        TestS3JerseyClient s3Client = new TestS3JerseyClient(s3Config);

        // verify settings in raw apache client
        // first find the handler in the chain
        Client jerseyClient = s3Client.getClient();

        // get the connection manager
        PoolingHttpClientConnectionManager apacheConnMgr = (PoolingHttpClientConnectionManager) jerseyClient.getConfiguration().getProperty(ApacheClientProperties.CONNECTION_MANAGER);
        Assert.assertNotNull(apacheConnMgr);
        // check limit settings
        Assert.assertEquals(connectionLimitPerHost, apacheConnMgr.getDefaultMaxPerRoute());
        Assert.assertEquals(connectionLimitTotal, apacheConnMgr.getMaxTotal());
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
