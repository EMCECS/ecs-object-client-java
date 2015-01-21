package com.emc.object.s3.jersey;

import com.emc.object.GenericRequest;
import com.emc.object.Method;
import com.emc.object.ObjectRequest;
import com.emc.object.s3.*;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.*;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.SmartClientFactory;
import com.emc.rest.smart.SmartConfig;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import java.net.URI;

public class S3JerseyClient extends AbstractS3Client {
    protected Client client;

    public S3JerseyClient(S3Config s3Config) {
        super(s3Config);

        // disable Jersey's strict HTTP compliance validation (so we can PUT without an entity)
        s3Config.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, "true");

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

        URI endpoint = s3Config.getEndpoints().get(0);
        if (s3Config.property(S3Config.PROPERTY_POLL_PROTOCOL) != null)
            hostListProvider.setProtocol(s3Config.propAsString(S3Config.PROPERTY_POLL_PROTOCOL));
        else
            hostListProvider.setProtocol(endpoint.getScheme());

        if (s3Config.property(S3Config.PROPERTY_POLL_PORT) != null) {
            try {
                hostListProvider.setPort(Integer.parseInt(s3Config.propAsString(S3Config.PROPERTY_POLL_PORT)));
            } catch (NumberFormatException e) {
                throw new RuntimeException(String.format("invalid poll port (%s=%s)",
                        S3Config.PROPERTY_POLL_PORT, s3Config.propAsString(S3Config.PROPERTY_POLL_PORT)), e);
            }
        } else
            hostListProvider.setPort(endpoint.getPort());

        // S.C. - CLIENT CREATION
        client = SmartClientFactory.createSmartClient(smartConfig);

        // S.C. - FILTER REGISTRATION
        client.register(new NamespaceRequestFilter(s3Config), Priorities.HEADER_DECORATOR);
        client.register(new BucketRequestFilter(s3Config), Priorities.HEADER_DECORATOR);
        client.register(new AuthorizationRequestFilter(s3Config), Priorities.HEADER_DECORATOR);
        client.register(new ErrorResponseFilter());
    }

    @Override
    public ListDataNode listDataNodes() {
        return executeRequest(client, new GenericRequest(Method.GET, "", "endpoint"), ListDataNode.class);
    }

    @Override
    public ListBucketsResult listBuckets(ListBucketsRequest request) {
        return executeRequest(client, request, ListBucketsResult.class);
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            executeRequest(client, new GenericBucketRequest(Method.HEAD, null).withBucketName(bucketName), null);
            return true;
        } catch (S3Exception e) {
            if (e.getResponse().getStatus() == RestUtil.STATUS_REDIRECT) return true;
            if (e.getResponse().getStatus() == RestUtil.STATUS_NOT_FOUND) return false;
            // TODO: do we return true on a 403??
            throw e;
        }
    }

    @Override
    public void createBucket(CreateBucketRequest request) {
        executeRequest(client, request, null);
    }

    @Override
    public void deleteBucket(String bucketName) {
        executeRequest(client, new GenericBucketRequest(Method.DELETE, null).withBucketName(bucketName), null);
    }

    @Override
    public void setBucketAcl(SetBucketAclRequest request) {
        executeRequest(client, request, null);
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName) {
        S3Request request = new GenericBucketRequest(Method.GET, "acl").withBucketName(bucketName);
        return executeRequest(client, request, AccessControlList.class);
    }

    @Override
    public void setBucketCors(String bucketName, CorsConfiguration corsConfiguration) {
        S3Request request = new GenericBucketEntityRequest<>(Method.PUT, "cors",
                corsConfiguration, RestUtil.TYPE_APPLICATION_XML).withBucketName(bucketName);
        executeRequest(client, request, null);
    }

    @Override
    public CorsConfiguration getBucketCors(String bucketName) {
        S3Request request = new GenericBucketRequest(Method.GET, "cors").withBucketName(bucketName);
        return executeRequest(client, request, CorsConfiguration.class);
    }

    @Override
    public void deleteBucketCors(String bucketName) {
        executeRequest(client, new GenericBucketRequest(Method.DELETE, "cors").withBucketName(bucketName), null);
    }

    @Override
    public void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration) {
        S3Request request = new GenericBucketEntityRequest<>(Method.PUT, "lifecycle",
                lifecycleConfiguration, RestUtil.TYPE_APPLICATION_XML).withBucketName(bucketName);
        executeRequest(client, request, null);
    }

    @Override
    public LifecycleConfiguration getBucketLifecycle(String bucketName) {
        S3Request request = new GenericBucketRequest(Method.GET, "lifecycle").withBucketName(bucketName);
        return executeRequest(client, request, LifecycleConfiguration.class);
    }

    @Override
    public void deleteBucketLifecycle(String bucketName) {
        executeRequest(client, new GenericBucketRequest(Method.DELETE, "lifecycle").withBucketName(bucketName), null);
    }

    @Override
    public LocationConstraint getBucketLocation(String bucketName) {
        S3Request request = new GenericBucketRequest(Method.GET, "location").withBucketName(bucketName);
        return executeRequest(client, request, LocationConstraint.class);
    }

    @Override
    public void setBucketVersioning(String bucketName, VersioningConfiguration versioningConfiguration) {
        S3Request request = new GenericBucketEntityRequest<>(Method.PUT, "versioning",
                versioningConfiguration, RestUtil.TYPE_APPLICATION_XML).withBucketName(bucketName);
        executeRequest(client, request, null);
    }

    @Override
    public VersioningConfiguration getBucketVersioning(String bucketName) {
        S3Request request = new GenericBucketRequest(Method.GET, "versioning").withBucketName(bucketName);
        return executeRequest(client, request, VersioningConfiguration.class);
    }

    @Override
    public ListMultipartUploadsResult listMultipartUploads(ListMultipartUploadsRequest request) {
        return executeRequest(client, request, ListMultipartUploadsResult.class);
    }

    @Override
    public ListObjectsResult listObjects(ListObjectsRequest request) {
        return executeRequest(client, request, ListObjectsResult.class);
    }

    @Override
    public ListVersionsResult listVersions(ListVersionsRequest request) {
        return executeRequest(client, request, ListVersionsResult.class);
    }

    @Override
    public void createObject(String bucketName, final String key, Object content, String contentType) {
        if (contentType == null) contentType = RestUtil.DEFAULT_CONTENT_TYPE; // TODO: infer content type ??
        AbstractBucketRequest request = new GenericBucketEntityRequest<Object>(Method.PUT, null, content, contentType) {
            @Override
            public String getPath() {
                return key;
            }
        }.withBucketName(bucketName);
        executeRequest(client, request, null);
    }

    @Override
    public void deleteObject(String bucketName, final String key) {
        S3Request request = new GenericBucketRequest(Method.DELETE, null) {
            @Override
            public String getPath() {
                return key;
            }
        }.withBucketName(bucketName);
        executeRequest(client, request, null);
    }

    @Override
    protected Invocation.Builder buildRequest(Client client, ObjectRequest request) {
        Invocation.Builder builder = super.buildRequest(client, request); // this will set namespace

        // set bucket
        if (request instanceof AbstractBucketRequest)
            builder.property(S3Constants.PROPERTY_BUCKET_NAME, ((AbstractBucketRequest) request).getBucketName());

        return builder;
    }
}
