package com.emc.object.s3.jersey;

import com.emc.object.s3.AbstractS3Client;
import com.emc.object.s3.S3Config;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.ListObjectsRequest;
import com.emc.object.util.S3HostListProvider;
import com.emc.rest.smart.SmartClientFactory;
import com.emc.rest.smart.SmartConfig;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class S3JerseyClient extends AbstractS3Client {
    protected S3Config s3Config;
    protected Client client;

    public S3JerseyClient(S3Config s3Config) {
        this.s3Config = s3Config;

        // SMART CLIENT SETUP

        SmartConfig smartConfig = s3Config.toSmartConfig();

        // S.C. - ENDPOINT POLLING
        // create a separate client for getting the node list. use any client config parameters set on s3Config
        ClientConfig clientConfig = new ClientConfig();
        for (String key : s3Config.getProperties().keySet())
            clientConfig.property(key, s3Config.property(key));
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        Client pollingClient = JerseyClientBuilder.newClient(clientConfig);

        // create a host list provider based on the S3 ?endpoint call (will use the pollingClient we just made)
        S3HostListProvider hostListProvider = new S3HostListProvider(pollingClient, smartConfig.getLoadBalancer(),
                s3Config.getIdentity(), s3Config.getSecretKey());
        smartConfig.setHostListProvider(hostListProvider);

        if (s3Config.property(S3Config.PROPERTY_POLL_PROTOCOL) != null)
            hostListProvider.setProtocol(s3Config.propAsString(S3Config.PROPERTY_POLL_PROTOCOL));

        if (s3Config.property(S3Config.PROPERTY_POLL_PORT) != null) {
            try {
                hostListProvider.setPort(Integer.parseInt(s3Config.propAsString(S3Config.PROPERTY_POLL_PORT)));
            } catch (NumberFormatException e) {
                throw new RuntimeException(String.format("invalid poll port (%s=%s)",
                        S3Config.PROPERTY_POLL_PORT, s3Config.propAsString(S3Config.PROPERTY_POLL_PORT)), e);
            }
        }

        // S.C. - CLIENT CREATION
        client = SmartClientFactory.createSmartClient(smartConfig);

        // S.C. - PROVIDER REGISTRATION
        client.register(new NamespaceRequestFilter(s3Config), Priorities.HEADER_DECORATOR);
        client.register(new BucketRequestFilter(s3Config), Priorities.HEADER_DECORATOR);
        client.register(new AuthorizationRequestFilter(s3Config), Priorities.HEADER_DECORATOR);
        client.register(new ErrorResponseFilter());
    }

    @Override
    public ListBucketsResult listBuckets() {
        Response response = client.target(s3Config.resolvePath("/", null)).request().get();

        ListBucketsResult serviceInformation = response.readEntity(ListBucketsResult.class);

        response.close();

        return serviceInformation;
    }

    @Override
    public void deleteBucket(String bucketName) {

    }

    @Override
    public void deleteBucketCors(String bucketName) {

    }

    @Override
    public void deleteBucketLifecycle(String bucketName) {

    }

    @Override
    public ListObjectsResult listObjects(ListObjectsRequest request) {
        return null;
    }

    @Override
    public ListObjectsResult listObjects(String bucketName) {
        return null;
    }

    @Override
    public ListObjectsResult listObjects(String bucketName, String prefix) {
        return null;
    }

    @Override
    public CorsConfiguration getBucketCors(String bucketName) {
        return null;
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName) {
        return null;
    }

    @Override
    public LifecycleConfiguration getBucketLifecycle(String bucketName) {
        return null;
    }

    @Override
    public boolean bucketExists(String bucketName) {
        return false;
    }

    @Override
    public void createBucket(String bucketName) {

    }

    @Override
    public void setBucketCors(String bucketName, CorsConfiguration corsConfiguration) {

    }

    @Override
    public void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration) {

    }
}
