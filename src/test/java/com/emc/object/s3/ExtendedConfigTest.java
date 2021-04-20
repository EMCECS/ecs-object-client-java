package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.util.TestConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.junit.Assert;
import org.junit.Test;

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
        org.apache.http.impl.conn.PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(connectionLimitPerHost);
        connectionManager.setMaxTotal(connectionLimitTotal);

        // set connection manager property in config
        // (this will get passed down to the handler by the smart client factory)
        s3Config.setProperty(ApacheHttpClient4Config.PROPERTY_CONNECTION_MANAGER, connectionManager);

        TestS3JerseyClient s3Client = new TestS3JerseyClient(s3Config);

        // verify settings in raw apache client
        // first find the handler in the chain
        Client jerseyClient = s3Client.getClient();
        ClientHandler handler = jerseyClient.getHeadHandler();
        while (handler instanceof ClientFilter) {
            handler = ((ClientFilter) handler).getNext();
        }
        // apache handler should be right after the filters
        ApacheHttpClient4Handler apacheHandler = (ApacheHttpClient4Handler) handler;
        // get the raw client
        HttpClient httpClient = apacheHandler.getHttpClient();
        // get the connection manager
        ClientConnectionManager apacheConnMgr = httpClient.getConnectionManager();
        Assert.assertTrue(apacheConnMgr instanceof PoolingClientConnectionManager);
        // check limit settings
        Assert.assertEquals(connectionLimitPerHost, ((PoolingClientConnectionManager) apacheConnMgr).getDefaultMaxPerRoute());
        Assert.assertEquals(connectionLimitTotal, ((PoolingClientConnectionManager) apacheConnMgr).getMaxTotal());
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
