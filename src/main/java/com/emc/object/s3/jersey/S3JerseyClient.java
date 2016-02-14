/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3.jersey;

import com.emc.object.*;
import com.emc.object.s3.*;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.request.*;
import com.emc.object.util.RestUtil;
import com.emc.rest.smart.LoadBalancer;
import com.emc.rest.smart.SmartClientFactory;
import com.emc.rest.smart.SmartConfig;
import com.emc.rest.smart.SmartFilter;
import com.emc.rest.smart.ecs.EcsHostListProvider;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Reference implementation of S3Client.
 * <p>
 * This implementation uses the JAX-RS reference implementation (Jersey) as it's REST client.  When sending or
 * receiving data, the following content handlers are supported by default.  Be sure to use the appropriate content-type
 * associated with each object type or the handlers will not understand the request.
 * <p>
 * <table>
 * <tr><th>Object Type (class)</th><th>Expected Content-Type(s)</th></tr>
 * <tr><td>byte[]</td><td>*any*</td></tr>
 * <tr><td>java.lang.String</td><td>*any*</td></tr>
 * <tr><td>java.io.File (send-only)</td><td>*any*</td></tr>
 * <tr><td>java.io.InputStream (send-only)</td><td>*any*</td></tr>
 * <tr><td>any annotated JAXB root element bean</td><td>text/xml, application/xml</td></tr>
 * </table>
 * <p>
 * Also keep in mind that you can always send/receive byte[] and do your own conversion.
 * <p>
 * To use, simply pass a new {@link S3Config} object to the constructor like so:
 * <pre>
 *     // for client-side load balancing and direct connection to all nodes
 *     //   single-VDC (client will auto-discover the remaining nodes):
 *     S3Config config1 = new S3Config(Protocol.HTTP, "10.10.10.11", "10.10.10.12");
 *     //   multiple VDCs (client will auto-discover remaining nodes in specified VDCs):
 *     Vdc boston = new Vdc("10.10.10.11", "10.10.10.12").withName("Boston");
 *     Vdc seattle = new Vdc("10.20.20.11", "10.20.20.12").withName("Seattle");
 *     S3Config config2 = new S3Config(Protocol.HTTPS, boston, seattle);
 *
 *     // to use a load balancer will full wildcard DNS setup
 *     S3Config config3 = new S3Config(new URI("https://s3.company.com")).withUseVHost(true);
 *
 *     // in all cases, you need to provide your credentials
 *     configX.withIdentity("my_full_token_id").withSecretKey("my_secret_key");
 *     S3Client s3Client = new S3JerseyClient(configX);
 * </pre>
 * <p>
 * To create an object, simply pass the object in to one of the putObject methods. The object type must be one of
 * the supported types above.
 * <pre>
 *     String stringContent = "Hello World!";
 *     s3Client.putObject("my-bucket", "my-key", stringContent, "text/plain");
 *
 *     File fileContent = new File( "spreadsheet.xls" );
 *     s3Client.putObject("my-bucket", "my-data", fileContent, "application/vnd.ms-excel");
 *
 *     byte[] binaryContent;
 *     ... // load binary content to store as an object
 *     s3Client.putObject("my-bucket", "my-bits", binaryContent, null ); // default content-type is application/octet-stream
 * </pre>
 * <p>
 * To read an object, specify the type of object you want to receive from a readObject method. The same rules apply to
 * this type.
 * <pre>
 *     String stringContent = s3Client.readObject("my-bucket", "my-key", String.class);
 *
 *     byte[] fileContent = s3Client.readObject("my-bucket", "my-data", byte[].class);
 *     // do something with file content (stream to client? save in local filesystem?)
 *
 *     byte[] binaryContent = s3Client.readObject("my-bucket", "my-bits", byte[].class);
 * </pre>
 * <p>
 * <em>Performance</em>
 * <p>
 * If you are experiencing performance issues, you might try tuning Jersey's IO buffer size, which defaults to 8k.
 * <pre>
 *     System.setProperty(ReaderWriter.BUFFER_SIZE_SYSTEM_PROPERTY, "" + 128 * 1024); // 128k
 * </pre>
 * You can also try using Jersey's URLConnectionClientHandler, but be aware that this handler does not support
 * <code>Expect: 100-Continue</code> behavior if that is important to you. You should also increase
 * <code>http.maxConnections</code> to match your thread count.
 * <pre>
 *     System.setProperty("http.maxConnections", "" + 32); // if you have 32 threads
 *     S3Client s3Client = new S3JerseyClient(configX, new URLConnectionClientHandler());
 * </pre>
 */
public class S3JerseyClient extends AbstractJerseyClient implements S3Client {
    protected S3Config s3Config;
    protected Client client;
    protected LoadBalancer loadBalancer;

    public S3JerseyClient(S3Config s3Config) {
        this(s3Config, null);
    }

    /**
     * Provide a specific Jersey ClientHandler implementation (default is ApacheHttpClient4Handler). If you experience
     * performance problems, you might try using URLConnectionClientHandler, but note that it will not support the
     * Expect: 100-Continue header. Also note that when using that handler, you should set the "http.maxConnections"
     * system property to match your thread count (default is only 5).
     */
    public S3JerseyClient(S3Config config, ClientHandler clientHandler) {
        super(new S3Config(config)); // deep-copy config so that two clients don't share the same host lists (SDK-122)
        s3Config = (S3Config) super.getObjectConfig();

        SmartConfig smartConfig = s3Config.toSmartConfig();
        loadBalancer = smartConfig.getLoadBalancer();

        // creates a standard (non-load-balancing) jersey client
        if (clientHandler == null) {
            client = SmartClientFactory.createStandardClient(smartConfig);
        } else {
            client = SmartClientFactory.createStandardClient(smartConfig, clientHandler);
        }

        if (s3Config.isSmartClient()) {
            // SMART CLIENT SETUP

            // S.C. - ENDPOINT POLLING
            // create a host list provider based on the S3 ?endpoint call (will use the standard client we just made)
            EcsHostListProvider hostListProvider = new EcsHostListProvider(client, loadBalancer,
                    s3Config.getIdentity(), s3Config.getSecretKey());
            smartConfig.setHostListProvider(hostListProvider);

            if (s3Config.getProperty(S3Config.PROPERTY_POLL_PROTOCOL) != null)
                hostListProvider.setProtocol(s3Config.getPropAsString(S3Config.PROPERTY_POLL_PROTOCOL));
            else
                hostListProvider.setProtocol(s3Config.getProtocol().toString());

            if (s3Config.getProperty(S3Config.PROPERTY_POLL_PORT) != null) {
                try {
                    hostListProvider.setPort(Integer.parseInt(s3Config.getPropAsString(S3Config.PROPERTY_POLL_PORT)));
                } catch (NumberFormatException e) {
                    throw new RuntimeException(String.format("invalid poll port (%s=%s)",
                            S3Config.PROPERTY_POLL_PORT, s3Config.getPropAsString(S3Config.PROPERTY_POLL_PORT)), e);
                }
            } else {
                hostListProvider.setPort(s3Config.getPort());
            }

            // S.C. - VDC CONFIGURATION
            hostListProvider.setVdcs(s3Config.getVdcs());

            // S.C. - GEO-PINNING
            if (s3Config.isGeoPinningEnabled()) loadBalancer.withVetoRules(new GeoPinningRule());

            // S.C. - RETRY CONFIG
            if (s3Config.isRetryEnabled())
                smartConfig.setProperty(SmartClientFactory.DISABLE_APACHE_RETRY, Boolean.TRUE);

            // S.C. - CHUNKED ENCODING (match ECS buffer size)
            smartConfig.setProperty(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, s3Config.getChunkedEncodingSize());

            // S.C. - CLIENT CREATION
            // create a load-balancing jersey client
            if (clientHandler == null) {
                client = SmartClientFactory.createSmartClient(smartConfig);
            } else {
                client = SmartClientFactory.createSmartClient(smartConfig, clientHandler);
            }
        }

        // jersey filters
        client.addFilter(new ErrorFilter());
        if (s3Config.getFaultInjectionRate() > 0.0f)
            client.addFilter(new FaultInjectionFilter(s3Config.getFaultInjectionRate()));
        if (s3Config.isGeoPinningEnabled()) client.addFilter(new GeoPinningFilter(s3Config));
        if (s3Config.isRetryEnabled()) client.addFilter(new RetryFilter(s3Config)); // replaces the apache retry handler
        if (s3Config.isChecksumEnabled()) client.addFilter(new ChecksumFilter());
        client.addFilter(new AuthorizationFilter(s3Config));
        client.addFilter(new BucketFilter(s3Config));
        client.addFilter(new NamespaceFilter(s3Config));
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize(); // make sure we call super.finalize() no matter what!
        }
    }

    /**
     * @deprecated (2.0.3) use destroy() instead
     */
    @Override
    public void shutdown() {
        destroy();
    }

    /**
     * Destroy the client. Any system resources associated with the client
     * will be cleaned up.
     * <p/>
     * This method must be called when there are not responses pending otherwise
     * undefined behavior will occur.
     * <p/>
     * The client must not be reused after this method is called otherwise
     * undefined behavior will occur.
     */
    @Override
    public void destroy() {
        SmartClientFactory.destroy(client);
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public ListDataNode listDataNodes() {
        return executeRequest(client, new ObjectRequest(Method.GET, "", "endpoint"), ListDataNode.class);
    }

    @Override
    public PingResponse pingNode(String host) {
        return pingNode(s3Config.getProtocol(), host, s3Config.getPort());
    }

    @Override
    public PingResponse pingNode(Protocol protocol, String host, int port) {
        String portStr = (port > 0) ? ":" + port : "";
        WebResource resource = client.resource(String.format("%s://%s%s/?ping", protocol.name().toLowerCase(), host, portStr));
        resource.setProperty(SmartFilter.BYPASS_LOAD_BALANCER, true);
        return resource.get(PingResponse.class);
    }

    @Override
    public ListBucketsResult listBuckets() {
        return listBuckets(new ListBucketsRequest());
    }

    @Override
    public ListBucketsResult listBuckets(ListBucketsRequest request) {
        return executeRequest(client, request, ListBucketsResult.class);
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            executeAndClose(client, new GenericBucketRequest(Method.HEAD, bucketName, null));
            return true;
        } catch (S3Exception e) {
            switch (e.getHttpCode()) {
                case RestUtil.STATUS_REDIRECT:
                case RestUtil.STATUS_UNAUTHORIZED:
                    return true;
                case RestUtil.STATUS_NOT_FOUND:
                    return false;
                default:
                    throw e;
            }
        }
    }

    @Override
    public void createBucket(String bucketName) {
        createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public void createBucket(CreateBucketRequest request) {
        executeAndClose(client, request);
    }

    @Override
    public BucketInfo getBucketInfo(String bucketName) {
        BucketInfo result = new BucketInfo();
        result.setBucketName(bucketName);
        fillResponseEntity(result, executeAndClose(client, new GenericBucketRequest(Method.HEAD, bucketName, null)));
        return result;
    }

    @Override
    public void deleteBucket(String bucketName) {
        executeAndClose(client, new GenericBucketRequest(Method.DELETE, bucketName, null));
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl) {
        setBucketAcl(new SetBucketAclRequest(bucketName).withAcl(acl));
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAcl cannedAcl) {
        setBucketAcl(new SetBucketAclRequest(bucketName).withCannedAcl(cannedAcl));
    }

    @Override
    public void setBucketAcl(SetBucketAclRequest request) {
        executeAndClose(client, request);
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName) {
        ObjectRequest request = new GenericBucketRequest(Method.GET, bucketName, "acl");
        return executeRequest(client, request, AccessControlList.class);
    }

    @Override
    public void setBucketCors(String bucketName, CorsConfiguration corsConfiguration) {
        ObjectRequest request = new GenericBucketEntityRequest<CorsConfiguration>(
                Method.PUT, bucketName, "cors", corsConfiguration).withContentType(RestUtil.TYPE_APPLICATION_XML);
        executeAndClose(client, request);
    }

    @Override
    public CorsConfiguration getBucketCors(String bucketName) {
        ObjectRequest request = new GenericBucketRequest(Method.GET, bucketName, "cors");
        try {
            return executeRequest(client, request, CorsConfiguration.class);
        } catch (S3Exception e) {
            if ("NoSuchCORSConfiguration".equals(e.getErrorCode())) return null;
            throw e;
        }
    }

    @Override
    public void deleteBucketCors(String bucketName) {
        executeAndClose(client, new GenericBucketRequest(Method.DELETE, bucketName, "cors"));
    }

    @Override
    public void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration) {
        ObjectRequest request = new GenericBucketEntityRequest<LifecycleConfiguration>(
                Method.PUT, bucketName, "lifecycle", lifecycleConfiguration).withContentType(RestUtil.TYPE_APPLICATION_XML);
        executeAndClose(client, request);
    }

    @Override
    public LifecycleConfiguration getBucketLifecycle(String bucketName) {
        ObjectRequest request = new GenericBucketRequest(Method.GET, bucketName, "lifecycle");
        try {
            return executeRequest(client, request, LifecycleConfiguration.class);
        } catch (S3Exception e) {
            if ("NoSuchBucketPolicy".equals(e.getErrorCode())) return null;
            throw e;
        }
    }

    @Override
    public void deleteBucketLifecycle(String bucketName) {
        executeAndClose(client, new GenericBucketRequest(Method.DELETE, bucketName, "lifecycle"));
    }

    @Override
    public LocationConstraint getBucketLocation(String bucketName) {
        ObjectRequest request = new GenericBucketRequest(Method.GET, bucketName, "location");
        return executeRequest(client, request, LocationConstraint.class);
    }

    @Override
    public void setBucketVersioning(String bucketName, VersioningConfiguration versioningConfiguration) {
        ObjectRequest request = new GenericBucketEntityRequest<VersioningConfiguration>(
                Method.PUT, bucketName, "versioning", versioningConfiguration).withContentType(RestUtil.TYPE_APPLICATION_XML);
        executeAndClose(client, request);
    }

    @Override
    public VersioningConfiguration getBucketVersioning(String bucketName) {
        ObjectRequest request = new GenericBucketRequest(Method.GET, bucketName, "versioning");
        return executeRequest(client, request, VersioningConfiguration.class);
    }

    @Override
    public void setBucketStaleReadAllowed(String bucketName, final boolean staleReadAllowed) {
        ObjectRequest request = new GenericBucketRequest(Method.PUT, bucketName, S3Constants.PARAM_IS_STALE_ALLOWED) {
            @Override
            public Map<String, List<Object>> getHeaders() {
                Map<String, List<Object>> headers = super.getHeaders();
                RestUtil.putSingle(headers, RestUtil.EMC_STALE_READ_ALLOWED, staleReadAllowed);
                return headers;
            }
        };
        executeAndClose(client, request);
    }

    @Override
    public MetadataSearchList listBucketMetadataSearchKeys(String bucketName) {
        ObjectRequest request = new GenericBucketRequest(Method.GET, bucketName, "searchmetadata");
        return executeRequest(client, request, MetadataSearchList.class);
    }

    @Override
    public QueryObjectsResult queryObjects(QueryObjectsRequest request) {
        String query = request.getQuery();
        if(query == null || query.isEmpty()) {
            throw new IllegalArgumentException("QueryObjectsRequest must contain a query expression.");
        }
        QueryObjectsResult result = executeRequest(client, request, QueryObjectsResult.class);
        result.setQuery(query);
        result.setAttributes(request.getAttributes());
        result.setSorted(request.getSorted());
        result.setIncludeOlderVersions(request.getIncludeOlderVersions());
        return result;
    }

    @Override
    public QueryObjectsResult queryMoreObjects(QueryObjectsResult lastResult) {
        return queryObjects(new QueryObjectsRequest(lastResult.getBucketName())
                .withQuery(lastResult.getQuery())
                .withAttributes(lastResult.getAttributes())
                .withSorted(lastResult.getSorted())
                .withIncludeOlderVersions(lastResult.getIncludeOlderVersions())
                .withMaxKeys(lastResult.getMaxKeys())
                .withMarker(lastResult.getNextMarker()));
    }

    @Override
    public ListObjectsResult listObjects(String bucketName) {
        return listObjects(new ListObjectsRequest(bucketName));
    }

    @Override
    public ListObjectsResult listObjects(String bucketName, String prefix) {
        return listObjects(new ListObjectsRequest(bucketName).withPrefix(prefix));
    }

    @Override
    public ListObjectsResult listObjects(ListObjectsRequest request) {
        ListObjectsResult result = executeRequest(client, request, ListObjectsResult.class);
        if (result.isTruncated() && result.getNextMarker() == null)
            result.setNextMarker(result.getObjects().get(result.getObjects().size() - 1).getKey());
        return result;
    }

    @Override
    public ListObjectsResult listMoreObjects(ListObjectsResult lastResult) {
        return listObjects(new ListObjectsRequest(lastResult.getBucketName())
                .withPrefix(lastResult.getPrefix())
                .withDelimiter(lastResult.getDelimiter())
                .withEncodingType(lastResult.getEncodingType())
                .withMaxKeys(lastResult.getMaxKeys())
                .withMarker(lastResult.getNextMarker()));
    }

    @Override
    public ListVersionsResult listVersions(String bucketName, String prefix) {
        return listVersions(new ListVersionsRequest(bucketName).withPrefix(prefix));
    }

    @Override
    public ListVersionsResult listVersions(ListVersionsRequest request) {
        return executeRequest(client, request, ListVersionsResult.class);
    }

    @Override
    public ListVersionsResult listMoreVersions(ListVersionsResult lastResult) {
        return listVersions(new ListVersionsRequest(lastResult.getBucketName())
                .withPrefix(lastResult.getPrefix())
                .withDelimiter(lastResult.getDelimiter())
                .withEncodingType(lastResult.getEncodingType())
                .withMaxKeys(lastResult.getMaxKeys())
                .withKeyMarker(lastResult.getNextKeyMarker())
                .withVersionIdMarker(lastResult.getNextVersionIdMarker()));
    }

    @Override
    public void putObject(String bucketName, String key, Object content, String contentType) {
        S3ObjectMetadata metadata = new S3ObjectMetadata().withContentType(contentType);
        putObject(new PutObjectRequest(bucketName, key, content).withObjectMetadata(metadata));
    }

    @Override
    public void putObject(String bucketName, String key, Range range, Object content) {
        putObject(new PutObjectRequest(bucketName, key, content).withRange(range));
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest request) {

        // enable checksum of the object
        request.property(RestUtil.PROPERTY_VERIFY_WRITE_CHECKSUM, Boolean.TRUE);

        PutObjectResult result = new PutObjectResult();
        fillResponseEntity(result, executeAndClose(client, request));
        return result;
    }

    @Override
    public long appendObject(String bucketName, String key, Object content) {
        return putObject(new PutObjectRequest(bucketName, key, content)
                .withRange(Range.fromOffset(-1))).getAppendOffset();
    }

    @Override
    public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String bucketName, String key) {
        return copyObject(new CopyObjectRequest(sourceBucketName, sourceKey, bucketName, key));
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest request) {
        return executeRequest(client, request, CopyObjectResult.class);
    }

    @Override
    public <T> T readObject(String bucketName, String key, Class<T> objectType) {
        return getObject(new GetObjectRequest(bucketName, key), objectType).getObject();
    }

    @Override
    public <T> T readObject(String bucketName, String key, String versionId, Class<T> objectType) {
        return getObject(new GetObjectRequest(bucketName, key).withVersionId(versionId), objectType).getObject();
    }

    @Override
    public InputStream readObjectStream(String bucketName, String key, Range range) {
        return getObject(new GetObjectRequest(bucketName, key).withRange(range), InputStream.class).getObject();
    }

    @Override
    public GetObjectResult<InputStream> getObject(String bucketName, String key) {
        return getObject(new GetObjectRequest(bucketName, key), InputStream.class);
    }

    @Override
    public <T> GetObjectResult<T> getObject(GetObjectRequest request, Class<T> objectType) {
        try {
            if (request.getRange() == null) {
                // enable checksum of the object (verification is handled in interceptor)
                request.property(RestUtil.PROPERTY_VERIFY_READ_CHECKSUM, Boolean.TRUE);
            }

            GetObjectResult<T> result = new GetObjectResult<T>();
            ClientResponse response = executeRequest(client, request);
            fillResponseEntity(result, response);
            result.setObject(response.getEntity(objectType));
            return result;
        } catch (S3Exception e) {
            // a 304 or 412 means If-* headers were used and a condition failed
            if (e.getHttpCode() == 304 || e.getHttpCode() == 412) return null;
            throw e;
        }
    }

    @Override
    public URL getPresignedUrl(String bucketName, String key, Date expirationTime) {
        return getPresignedUrl(new PresignedUrlRequest(Method.GET, bucketName, key, expirationTime));
    }

    @Override
    public URL getPresignedUrl(PresignedUrlRequest request) {
        return S3AuthUtil.generatePresignedUrl(request, s3Config);
    }

    @Override
    public void deleteObject(String bucketName, final String key) {
        executeAndClose(client, new S3ObjectRequest(Method.DELETE, bucketName, key, null));
    }

    @Override
    public void deleteVersion(String bucketName, String key, String versionId) {
        executeAndClose(client, new S3ObjectRequest(Method.DELETE, bucketName, key, "versionId=" + versionId));
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest request) {
        return executeRequest(client, request, DeleteObjectsResult.class);
    }

    @Override
    public void setObjectMetadata(String bucketName, String key, S3ObjectMetadata objectMetadata) {
        AccessControlList acl = getObjectAcl(bucketName, key);
        copyObject(new CopyObjectRequest(bucketName, key, bucketName, key).withAcl(acl).withObjectMetadata(objectMetadata));
    }

    @Override
    public S3ObjectMetadata getObjectMetadata(String bucketName, String key) {
        return getObjectMetadata(new GetObjectMetadataRequest(bucketName, key));
    }

    @Override
    public S3ObjectMetadata getObjectMetadata(GetObjectMetadataRequest request) {
        try {
            return S3ObjectMetadata.fromHeaders(executeAndClose(client, request).getHeaders());
        } catch (S3Exception e) {
            // a 304 or 412 means If-* headers were used and a condition failed
            if (e.getHttpCode() == 304 || e.getHttpCode() == 412) return null;
            throw e;
        }
    }

    @Override
    public void setObjectAcl(String bucketName, String key, AccessControlList acl) {
        setObjectAcl(new SetObjectAclRequest(bucketName, key).withAcl(acl));
    }

    @Override
    public void setObjectAcl(String bucketName, String key, CannedAcl cannedAcl) {
        setObjectAcl(new SetObjectAclRequest(bucketName, key).withCannedAcl(cannedAcl));
    }

    @Override
    public void setObjectAcl(SetObjectAclRequest request) {
        executeAndClose(client, request);
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key) {
        return getObjectAcl(new GetObjectAclRequest(bucketName, key));
    }

    @Override
    public AccessControlList getObjectAcl(GetObjectAclRequest request) {
        return executeRequest(client, request, AccessControlList.class);
    }

    @Override
    public ListMultipartUploadsResult listMultipartUploads(String bucketName) {
        return listMultipartUploads(new ListMultipartUploadsRequest(bucketName));
    }

    @Override
    public ListMultipartUploadsResult listMultipartUploads(ListMultipartUploadsRequest request) {
        return executeRequest(client, request, ListMultipartUploadsResult.class);
    }

    @Override
    public String initiateMultipartUpload(String bucketName, String key) {
        return initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, key)).getUploadId();
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
        return executeRequest(client, request, InitiateMultipartUploadResult.class);
    }

    @Override
    public ListPartsResult listParts(String bucketName, String key, String uploadId) {
        return listParts(new ListPartsRequest(bucketName, key, uploadId));
    }

    @Override
    public ListPartsResult listParts(ListPartsRequest request) {
        return executeRequest(client, request, ListPartsResult.class);
    }

    @Override
    public MultipartPartETag uploadPart(UploadPartRequest request) {
        return new MultipartPartETag(request.getPartNumber(), executeAndClose(client, request).getEntityTag().getValue());
    }

    @Override
    public CopyPartResult copyPart(CopyPartRequest request) {
        CopyPartResult result = executeRequest(client, request, CopyPartResult.class);
        result.setPartNumber(request.getPartNumber());
        return result;
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
        return executeRequest(client, request, CompleteMultipartUploadResult.class);
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest request) {
        executeAndClose(client, request);
    }

    @Override
    protected <T> T executeRequest(Client client, ObjectRequest request, Class<T> responseType) {
        ClientResponse response = executeRequest(client, request);
        try {
            T responseEntity = response.getEntity(responseType);
            fillResponseEntity(responseEntity, response);
            return responseEntity;
        } catch (ClientHandlerException e) {

            // some S3 responses return a 200 right away, but may fail and include an error XML package instead of the
            // expected entity. check for that here.
            try {
                throw ErrorFilter.parseErrorResponse(new StringReader(response.getEntity(String.class)), response.getStatus());
            } catch (Throwable t) {

                // must be a reader error
                throw e;
            }
        }
    }

    public S3Config getS3Config() {
        return s3Config;
    }
}
