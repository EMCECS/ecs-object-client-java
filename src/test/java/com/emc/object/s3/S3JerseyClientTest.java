package com.emc.object.s3;

import com.emc.object.AbstractClientTest;
import com.emc.object.s3.bean.ListBucketsResult;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.vipr.services.lib.ViprConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class S3JerseyClientTest extends AbstractClientTest {
    protected S3Client client;

    @Override
    protected void createBucket(String bucketName) throws Exception {
        client.createBucket(bucketName);
    }

    @Override
    protected void cleanUpBucket(String bucketName) throws Exception {
//        for (String key : client.listObjects(getTestBucket())) {
//            client.deleteObject(bucketName, key);
//        }
        client.deleteBucket(bucketName);
    }

    @Override
    public void initClient() throws Exception {
        Properties props = ViprConfig.getProperties();

        String accessKey = ViprConfig.getPropertyNotEmpty(props, ViprConfig.PROP_S3_ACCESS_KEY_ID);
        String secretKey = ViprConfig.getPropertyNotEmpty(props, ViprConfig.PROP_S3_SECRET_KEY);
        String endpoint = ViprConfig.getPropertyNotEmpty(props, ViprConfig.PROP_S3_ENDPOINT);
        String endpoints = props.getProperty(ViprConfig.PROP_S3_ENDPOINTS);

        S3Config s3Config = new S3Config();
        s3Config.setEndpoints(parseUris(endpoints == null ? endpoint : endpoints));
        s3Config.withIdentity(accessKey).withSecretKey(secretKey);
        s3Config.getProperties().put(S3Config.PROPERTY_POLL_PROTOCOL, "http");
        s3Config.getProperties().put(S3Config.PROPERTY_POLL_PORT, "9020");

        // TODO: remove when jersey request logging is fixed
        s3Config.getProperties().put(ApacheClientProperties.CONNECTION_MANAGER, new PoolingHttpClientConnectionManager());

        client = new S3JerseyClient(s3Config);
    }

    @Test
    public void testListBuckets() throws Exception {
        ListBucketsResult result = client.listBuckets();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOwner());
        Assert.assertNotNull(result.getBuckets());
    }

    public void sampleTest() throws Exception {
        // create some objects
        String key = "my-object";
//        client.createObject(getTestBucket(), key);

        // test some stuff

        // done (don't have to clean up after yourself)
    }

    protected List<URI> parseUris(String uriString) throws Exception {
        List<URI> uris = new ArrayList<>();
        for (String uri : uriString.split(",")) {
            uris.add(new URI(uri));
        }
        return uris;
    }
}
