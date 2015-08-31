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
package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.ecs.Vdc;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class S3JerseyClientTest extends AbstractS3ClientTest {
    private static final Logger l4j = Logger.getLogger(S3JerseyClientTest.class);

    @Override
    protected String getTestBucketPrefix() {
        return "s3-client-test";
    }

    @Override
    public void initClient() throws Exception {
        client = new S3JerseyClient(createS3Config());
    }

    @Test
    public void testListDataNodes() throws Exception {
        ListDataNode listDataNode = client.listDataNodes();
        Assert.assertNotNull(listDataNode.getVersionInfo());
        Assert.assertNotNull(listDataNode.getDataNodes());
        Assert.assertFalse(listDataNode.getDataNodes().isEmpty());
    }

    @Test
    public void testMultipleVdcs() throws Exception {
        S3Config config = createS3Config();

        Assume.assumeFalse(config.isUseVHost());

        try {
            client.listDataNodes();
        } catch (Exception e) {
            Assume.assumeNoException(e);
        }

        // just going to use the same VDC twice for lack of a geo env.
        List<? extends Host> hosts = config.getVdcs().get(0).getHosts();
        Vdc vdc1 = new Vdc("vdc1", new ArrayList<Host>(hosts)), vdc2 = new Vdc("vdc2", new ArrayList<Host>(hosts));

        String proxyUri = config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        config = new S3Config(config.getProtocol(), vdc1, vdc2).withPort(config.getPort())
                .withIdentity(config.getIdentity()).withSecretKey(config.getSecretKey());
        if (proxyUri != null) config.setProperty(ObjectConfig.PROPERTY_PROXY_URI, proxyUri);

        S3JerseyClient tempClient = new S3JerseyClient(config);

        Thread.sleep(1000); // wait for poll to complete

        // the client will clone the config, so we have to get new references
        vdc1 = tempClient.getS3Config().getVdcs().get(0);
        vdc2 = tempClient.getS3Config().getVdcs().get(1);

        Assert.assertTrue(vdc1.getHosts().size() > 1);
        Assert.assertTrue(vdc2.getHosts().size() > 1);
        Assert.assertEquals(vdc1.getHosts().size() + vdc2.getHosts().size(),
                tempClient.getLoadBalancer().getAllHosts().size());
    }

    @Test
    public void testCreateExistingBucket() throws Exception {
        try {
            client.createBucket(getTestBucket());
            Assert.fail("Fail was expected. Can NOT create a duplicate bucket");
        } catch (S3Exception e) {
            Assert.assertEquals("wrong error code for create existing bucket", "BucketAlreadyExists", e.getErrorCode());
        }
    }

    @Test
    public void testListBuckets() throws Exception {
        ListBucketsResult result = client.listBuckets();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOwner());
        Assert.assertNotNull(result.getBuckets());

        Bucket bucket = new Bucket();
        bucket.setName(getTestBucket());
        Assert.assertTrue(result.getBuckets().contains(bucket));
    }

    @Test
    public void testListBucketsReq() {
        ListBucketsRequest request = new ListBucketsRequest();
        ListBucketsResult result = client.listBuckets(request);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOwner());
        Assert.assertNotNull(result.getBuckets());

        Bucket bucket = new Bucket();
        bucket.setName(getTestBucket());
        Assert.assertTrue(result.getBuckets().contains(bucket));
    }
    
    @Test
    public void testBucketExists() throws Exception {
        Assert.assertTrue("Bucket " + getTestBucket() + " should exist but does NOT", client.bucketExists(getTestBucket()));
    }

    @Test
    public void testCreateBucketRequest() throws Exception {
        String bucketName = getTestBucket() + "-x";
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        client.createBucket(request);

        Assert.assertTrue(client.bucketExists(bucketName));
        this.cleanUpBucket(bucketName);
    }

    // TODO: blocked by STORAGE-7816
    @Ignore
    @Test
    public void testDeleteBucket() throws Exception {
        String bucketName = getTestBucket() + "-x";
        Assert.assertFalse("bucket should not exist " + bucketName, client.bucketExists(bucketName));

        client.createBucket(bucketName);
        Assert.assertTrue("failed to create bucket " + bucketName, client.bucketExists(bucketName));

        client.deleteBucket(bucketName);
        Assert.assertFalse("failed to delete bucket " + bucketName, client.bucketExists(bucketName));
    }


    @Test
    public void testDeleteBucketWithObjects() throws Exception {
        createTestObjects("prefix/", 5);
        try {
            client.deleteBucket(getTestBucket());
            Assert.fail("Test succeeds. Fail was expected. Can NOT delete bucket with existing objects");
        } catch (S3Exception e) {
            Assert.assertEquals("wrong error code for delete non-empty bucket", "BucketNotEmpty", e.getErrorCode());
        }
    }

    @Test
    public void testSetGetBucketAcl() throws Exception {
        String identity = createS3Config().getIdentity();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setBucketAcl(getTestBucket(), acl);

        this.assertAclEquals(acl, client.getBucketAcl(getTestBucket()));
    }

    @Ignore // TODO: blocked by STORAGE-7422
    @Test
    public void testSetBucketAclCanned() throws Exception {
        String identity = createS3Config().getIdentity();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setBucketAcl(getTestBucket(), CannedAcl.BucketOwnerFullControl);

        this.assertAclEquals(acl, client.getBucketAcl(getTestBucket()));
    }

    @Test
    public void testSetGetBucketCors() throws Exception {
        CorsRule cr0 = new CorsRule().withId("corsRuleTestId0");
        cr0.withAllowedOrigins("10.10.10.10").withAllowedMethods(CorsMethod.GET);

        CorsRule cr1 = new CorsRule().withId("corsRuleTestId1");
        cr1.withAllowedOrigins("10.10.10.10").withAllowedMethods(CorsMethod.GET);

        CorsRule cr2 = new CorsRule().withId("corsRuleTestId2");
        cr2.withAllowedOrigins("10.10.10.10").withAllowedMethods(CorsMethod.GET);

        CorsConfiguration cc = new CorsConfiguration().withCorsRules(cr0, cr1, cr2);
        client.setBucketCors(getTestBucket(), cc);

        CorsConfiguration ccVerify = client.getBucketCors(getTestBucket());
        Assert.assertNotNull("CorsConfiguration should NOT be null but is", ccVerify);
        Assert.assertEquals(cc.getCorsRules().size(), ccVerify.getCorsRules().size());

        for (CorsRule rule : cc.getCorsRules()) {
            Assert.assertTrue(ccVerify.getCorsRules().contains(rule));
        }
    }

    @Test
    public void testDeleteBucketCors() throws Exception {
        CorsRule cr0 = new CorsRule().withId("corsRuleTestId0");
        cr0.withAllowedOrigins("10.10.10.10").withAllowedMethods(CorsMethod.GET);

        CorsConfiguration cc = new CorsConfiguration().withCorsRules(cr0);
        client.setBucketCors(getTestBucket(), cc);

        Assert.assertNotNull(client.getBucketCors(getTestBucket()));

        client.deleteBucketCors(getTestBucket());

        Assert.assertNull(client.getBucketCors(getTestBucket()));
    }
    
    @Test
    public void testBucketLifecycle() throws Exception {
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 300);
        LifecycleConfiguration lc = new LifecycleConfiguration();
        lc.withRules(new LifecycleRule("archive-expires-180", "archive/", LifecycleRule.Status.Enabled, 180));

        client.setBucketLifecycle(getTestBucket(), lc);

        LifecycleConfiguration lc2 = client.getBucketLifecycle(getTestBucket());
        Assert.assertNotNull(lc2);
        Assert.assertEquals(lc.getRules().size(), lc2.getRules().size());

        for (LifecycleRule rule : lc.getRules()) {
            Assert.assertTrue(lc2.getRules().contains(rule));
        }

        lc.withRules(new LifecycleRule("armageddon", "", LifecycleRule.Status.Enabled, end.getTime()));

        client.setBucketLifecycle(getTestBucket(), lc);

        lc2 = client.getBucketLifecycle(getTestBucket());
        Assert.assertNotNull(lc2);
        Assert.assertEquals(lc.getRules().size(), lc2.getRules().size());

        for (LifecycleRule rule : lc.getRules()) {
            Assert.assertTrue(lc2.getRules().contains(rule));
        }

        client.deleteBucketLifecycle(getTestBucket());
        Assert.assertNull(client.getBucketLifecycle(getTestBucket()));
    }

    @Test
    public void testListObjects() throws Exception {
        String key = "foo", content = "Hello List!";
        client.putObject(getTestBucket(), key, content, null);

        ListObjectsResult result = client.listObjects(getTestBucket());
        Assert.assertNotNull("ListObjectsResult was null, but should NOT have been", result);

        List<S3Object> resultObjects = result.getObjects();
        Assert.assertNotNull("List<S3Object> was null, but should NOT have been", resultObjects);
        Assert.assertEquals(1, resultObjects.size());

        S3Object object = resultObjects.get(0);
        Assert.assertEquals(key, object.getKey());
        Assert.assertEquals((long) content.length(), object.getSize().longValue());
    }
    
    @Test
    public void testListObjectsWithPrefix() throws Exception {
        String prefix = "testPrefix/", key = "foo", content = "Hello List Prefix!";
        client.putObject(getTestBucket(), prefix + key, content, null);

        // test different prefix
        ListObjectsResult result = client.listObjects(getTestBucket(), "wrongPrefix/");
        Assert.assertNotNull("ListObjectsResult was null and should NOT be", result);
        Assert.assertEquals(0, result.getObjects().size());

        result = client.listObjects(getTestBucket(), prefix);
        Assert.assertNotNull("ListObjectsResult was null and should NOT be", result);
        Assert.assertEquals("The correct number of objects were NOT returned", 1, result.getObjects().size());

        List<S3Object> resultObjects = result.getObjects();
        Assert.assertNotNull("List<S3Object> was null, but should NOT have been", resultObjects);
        Assert.assertEquals(1, resultObjects.size());

        S3Object object = resultObjects.get(0);
        Assert.assertEquals(prefix + key, object.getKey());
        Assert.assertEquals((long) content.length(), object.getSize().longValue());
    }


    @Test
    public void testListObjectsPaging() throws Exception {
        int numObjects = 10;

        this.createTestObjects(null, numObjects);

        List<S3Object> objects = new ArrayList<S3Object>();
        ListObjectsResult result = null;
        int requestCount = 0;
        do {
            if (result == null) result = client.listObjects(new ListObjectsRequest(getTestBucket()).withMaxKeys(3));
            else result = client.listMoreObjects(result);
            objects.addAll(result.getObjects());
            requestCount++;
        } while (result.isTruncated());

        Assert.assertEquals("The correct number of objects were NOT returned", numObjects, objects.size());
        Assert.assertEquals("should be 4 pages", 4, requestCount);
    }

    @Test
    public void testListObjectsPagingWithPrefix() throws Exception {
        String myPrefix = "testPrefix/";
        int numObjects = 10;

        this.createTestObjects(myPrefix, numObjects);

        List<S3Object> objects = new ArrayList<S3Object>();
        ListObjectsResult result = null;
        int requestCount = 0;
        do {
            if (result == null) result = client.listObjects(new ListObjectsRequest(getTestBucket())
                    .withPrefix(myPrefix).withMaxKeys(3));
            else result = client.listMoreObjects(result);
            objects.addAll(result.getObjects());
            requestCount++;
        } while (result.isTruncated());

        Assert.assertEquals("The correct number of objects were NOT returned", numObjects, objects.size());
        Assert.assertEquals("should be 4 pages", 4, requestCount);
    }

    @Test
    public void testListAndReadVersions() throws Exception {
        // turn on versioning first
        client.setBucketVersioning(getTestBucket(),
                new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));

        // create a few versions of the same key
        String key = "prefix/foo", content = "Hello Versions!";
        client.putObject(getTestBucket(), key, content, null);
        client.deleteObject(getTestBucket(), key);
        client.putObject(getTestBucket(), key, content, null);

        // test different prefix
        ListVersionsResult result = client.listVersions(getTestBucket(), "wrongPrefix/");
        Assert.assertNotNull("ListObjectsResult was null and should NOT be", result);
        Assert.assertEquals(0, result.getVersions().size());

        result = client.listVersions(getTestBucket(), "prefix/");
        Assert.assertNotNull(result.getVersions());
        Assert.assertNotNull(result.getBucketName());

        List<AbstractVersion> versions = result.getVersions();
        Assert.assertNotNull(versions);
        Assert.assertEquals(3, versions.size());

        // should be version, delete-marker, version
        Assert.assertTrue(versions.get(0) instanceof Version);
        Assert.assertTrue(versions.get(1) instanceof DeleteMarker);
        Assert.assertTrue(versions.get(2) instanceof Version);

        for (AbstractVersion version : versions) {
            Assert.assertEquals(key, version.getKey());
            Assert.assertNotNull(version.getLastModified());
            Assert.assertNotNull(version.getOwner());
            Assert.assertNotNull(version.getVersionId());
            if (version instanceof Version) {
                Assert.assertEquals((long) content.length(), ((Version) version).getSize().longValue());
                Assert.assertEquals("b13f87dd03c70083eb3e98ca37372361", ((Version) version).getRawETag());
                Assert.assertEquals(content, client.readObject(getTestBucket(), key, version.getVersionId(), String.class));
            }
        }
    }

    @Test
    public void testListVersionsPagingPrefixDelim() throws Exception {
        // turn on versioning first
        client.setBucketVersioning(getTestBucket(),
                new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));

        // create a few versions of the same key
        String key = "prefix/foo", content = "Hello Versions!";
        client.putObject(getTestBucket(), key, content, null);
        client.deleteObject(getTestBucket(), key);
        client.putObject(getTestBucket(), key, content, null);

        // create key in sub-prefix
        client.putObject(getTestBucket(), "prefix/prefix2/bar", content, null);
        client.deleteObject(getTestBucket(), key);
        client.putObject(getTestBucket(), "prefix/prefix2/bar", content, null);

        ListVersionsRequest request = new ListVersionsRequest(getTestBucket()).withPrefix("prefix/")
                .withDelimiter("/").withMaxKeys(2);
        ListVersionsResult result = client.listVersions(request);

        Assert.assertEquals(2, result.getVersions().size());
        Assert.assertEquals(1, result.getCommonPrefixes().size());
        Assert.assertEquals("prefix/prefix2/", result.getCommonPrefixes().get(0));

        List<AbstractVersion> versions = result.getVersions();
        int callCount = 1;
        while (result.isTruncated()) {
            result = client.listMoreVersions(result);
            versions.addAll(result.getVersions());
            callCount++;
        }

        Assert.assertEquals(3, callCount);
        Assert.assertEquals(6, versions.size());
    }

    protected void createTestObjects(String prefix, int numObjects) throws Exception {
        if (prefix == null) prefix = "";

        byte[] content = new byte[5 * 1024];
        new Random().nextBytes(content);

        for(int i=0; i<numObjects; i++) {
            client.putObject(getTestBucket(), prefix + "TestObject_" + i, content, null);
        }
    }

    @Test
    public void testReadObject() throws Exception {
        String key1 = "/objectPrefix/testObject1";
        String key2 = "/objectPrefix/testObject2";
        String content1 = "Hello Object!", content2 = "Hello Object 2!!";

        client.putObject(getTestBucket(), key1, content1, "text/plain");
        Assert.assertEquals(content1, client.readObject(getTestBucket(), key1, String.class));

        client.putObject(getTestBucket(), key2, content2, "text/plain");
        Assert.assertEquals(content2, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testUpdateObjectWithRange() throws Exception {
        String key = "testRange";
        String content = "hello object!";
        int offset = content.indexOf("object");

        // create object
        client.putObject(getTestBucket(), key, content, "text/plain");

        // update fixed range
        String contentPart = "ranges!";
        Range range = Range.fromOffsetLength(offset, contentPart.length());
        client.putObject(getTestBucket(), key, range, (Object) contentPart);

        // update tail
        range = Range.fromOffset(offset);
        contentPart = "really long noun!";
        client.putObject(getTestBucket(), key, range, (Object) contentPart);

        Assert.assertEquals(content.substring(0, offset) + contentPart, client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testCreateObjectByteArray() throws Exception {
        byte[] data;
        Random random = new Random();

        data = new byte[15];
        random.nextBytes(data);
        client.putObject(getTestBucket(), "hello-bytes-small", data, null);
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), "hello-bytes-small", byte[].class));

        data = new byte[32 * 1024 - 1];
        random.nextBytes(data);
        client.putObject(getTestBucket(), "hello-bytes-less", data, null);
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), "hello-bytes-less", byte[].class));

        data = new byte[32 * 1024 + 1];
        random.nextBytes(data);
        client.putObject(getTestBucket(), "hello-bytes-more", data, null);
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), "hello-bytes-more", byte[].class));
    }

    @Test
    public void testCreateObjectString() throws Exception {
        String key = "string-test";
        String content = "Hello Strings!";
        client.putObject(getTestBucket(), key, content, "text/plain");
        Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testCreateObjectWithRequest() throws Exception {
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), "/objectPrefix/testObject1", "object content");
        PutObjectResult result = client.putObject(request);
        Assert.assertNotNull(result);
    }

    @Test
    public void testCreateObjectChunkedWithRequest() throws Exception {
        int size = 50000;
        byte[] data =  new byte[size];
        new Random().nextBytes(data);
        String dataStr = new String(data);
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), "/objectPrefix/testObject1", dataStr);
        //request.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

        //request.property(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, -1);

        request.property(ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING, Boolean.FALSE);
        PutObjectResult result = client.putObject(request);
        Assert.assertNotNull(result);
    }

    @Ignore // TODO: blocked by STORAGE-374
    @Test
    public void testCreateObjectWithMetadata() throws Exception {
        String key = "meta-test", content = "Hello Metadata!";
        String cc = "none", cd = "none", ce = "none", ct = "text/plain";
        Calendar expires = Calendar.getInstance();
        expires.add(Calendar.DATE, 1);
        expires.set(Calendar.MILLISECOND, 0);
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("meta1", "value1");
        userMeta.put("meta2", "value2");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().withContentType(ct);
        objectMetadata.withCacheControl(cc).withContentDisposition(cd).withContentEncoding(ce);
        objectMetadata.withHttpExpires(expires.getTime());
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(ct, objectMetadata.getContentType());
        Assert.assertEquals(cc, objectMetadata.getCacheControl());
        Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());
    }

    @Test
    public void testLargeObjectContentLength() throws Exception {
        String key = "large-object";
        int size = 1024 * 1024;
        Random random = new Random();
        byte[] bigData = new byte[size];
        random.nextBytes(bigData);
        client.putObject(getTestBucket(), key, bigData, null);

        GetObjectResult<byte[]> result = client.getObject(new GetObjectRequest(getTestBucket(), key), byte[].class);
        Assert.assertEquals("bad content-length", new Long(size), result.getObjectMetadata().getContentLength());
    }

    @Test
    public void testLargeFileUploader() throws Exception {
        String key = "large-file-uploader.bin";
        int size = 20 * 1024 * 1024 + 123; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        File file = File.createTempFile("large-file-uploader-test", null);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();

        LargeFileUploader uploader = new LargeFileUploader(client, getTestBucket(), key, file);

        // multipart
        uploader.doMultipartUpload();

        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));

        client.deleteObject(getTestBucket(), key);

        // parallel byte-range (also test metadata)
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().addUserMetadata("key", "value");
        uploader = new LargeFileUploader(client, getTestBucket(), key, file);
        uploader.setObjectMetadata(objectMetadata);
        uploader.doByteRangeUpload();

        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), key, byte[].class));
        Assert.assertEquals(objectMetadata.getUserMetadata(), client.getObjectMetadata(getTestBucket(), key).getUserMetadata());

        // test issue 1 (https://github.com/emcvipr/ecs-object-client-java/issues/1)
        objectMetadata = new S3ObjectMetadata();
        objectMetadata.withContentLength(size);
        uploader = new LargeFileUploader(client, getTestBucket(), key + ".2", file);
        uploader.setObjectMetadata(objectMetadata);
        uploader.doByteRangeUpload();
    }

    @Test
    public void testLargeFileDownloader() throws Exception {
        String key = "large-file-downloader.bin";
        int size = 20 * 1024 * 1024 + 179; // > 20MB
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        client.putObject(getTestBucket(), key, data, null);

        File file = File.createTempFile("large-file-uploader-test", null);
        file.deleteOnExit();
        LargeFileDownloader downloader = new LargeFileDownloader(client, getTestBucket(), key, file);
        downloader.run();

        byte[] readData = new byte[size];
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.read(readData);
        raf.close();

        Assert.assertArrayEquals(data, readData);
    }

    @Test
    public void testBucketLocation() throws Exception {
        LocationConstraint lc = client.getBucketLocation(getTestBucket());
        Assert.assertNotNull(lc);
        l4j.debug("Bucket location: " + lc.getRegion());
    }

    @Test
    public void testSetBucketVersioning() throws Exception {
        VersioningConfiguration vc = new VersioningConfiguration();
        vc.setStatus(VersioningConfiguration.Status.Enabled);

        client.setBucketVersioning(getTestBucket(), vc);

        VersioningConfiguration vcResult = client.getBucketVersioning(getTestBucket());
        Assert.assertEquals("status is wrong", vc.getStatus(), vcResult.getStatus());
    }

    @Test
    public void testSingleMultipartUploadMostSimpleOnePart() throws Exception {
        String key = "TestObject_" + UUID.randomUUID();
        int fiveKB = 5 * 1024;
        byte[] content1 = new byte[5 * 1024];
        new Random().nextBytes(content1);
        InputStream is1 = new ByteArrayInputStream(content1, 0, fiveKB);

        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);
        MultipartPartETag mp1 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1, is1));

        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        client.completeMultipartUpload(completionRequest);
    }

    @Test
    public void testSingleMultipartUploadListParts() throws Exception {
        String key = "TestObject_" + UUID.randomUUID();
        int fiveKB = 5 * 1024;
        byte[] content1 = new byte[5 * 1024];
        byte[] content2 = new byte[5 * 1024];
        byte[] content3 = new byte[5 * 1024];
        new Random().nextBytes(content1);
        new Random().nextBytes(content2);
        new Random().nextBytes(content3);
        InputStream is1 = new ByteArrayInputStream(content1, 0, fiveKB);
        InputStream is2 = new ByteArrayInputStream(content2, 0, fiveKB);
        InputStream is3 = new ByteArrayInputStream(content3, 0, fiveKB);

        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        MultipartPartETag mp1 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1, is1));

        MultipartPartETag mp2 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 2, is2));

        MultipartPartETag mp3 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 3, is3));

        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        parts.add(mp2);
        parts.add(mp3);


        ListPartsResult lpr = client.listParts(getTestBucket(), key, uploadId);

        List<MultipartPart> mpp = lpr.getParts();
        Assert.assertEquals(3, mpp.size());

        for (MultipartPart part: mpp) {
            //this does NOT assume that the list comes back in sequential order
            if (part.getPartNumber() == 1) {
                Assert.assertEquals(mp1.getETag(), mpp.get(0).getETag());
            }
            else if (part.getPartNumber() == 2) {
                Assert.assertEquals(mp2.getETag(), mpp.get(1).getETag());
            }
            else if (part.getPartNumber() == 3) {
                Assert.assertEquals(mp3.getETag(), mpp.get(2).getETag());
            }
            else {
                Assert.fail("Unknown Part number: " + part.getPartNumber());
            }
        }

        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        client.completeMultipartUpload(completionRequest);
    }


    @Test
    public void testMultiThreadMultipartUploadListPartsPagination() throws Exception {
        String key = "mpuListPartsTest";
        File file = createRandomTempFile(10 * 1024 * 1024 + 333); // 10MB+ (not a power of 2)
        int partSize = 2 * 1024 * 1024; // 2MB parts

        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        try {
            List<Future<MultipartPartETag>> futures = new ArrayList<Future<MultipartPartETag>>();
            ExecutorService executor = Executors.newFixedThreadPool(8);

            int partNumber = 1;
            long offset = 0, length = partSize;
            while (offset < file.length()) {
                if (offset + length > file.length()) length = file.length() - offset;
                final UploadFilePartRequest partRequest = new UploadFilePartRequest(getTestBucket(), key, uploadId, partNumber++);
                partRequest.withFile(file).withOffset(offset).withLength(length);
                futures.add(executor.submit(new Callable<MultipartPartETag>() {
                    @Override
                    public MultipartPartETag call() throws Exception {
                        return client.uploadPart(partRequest);
                    }
                }));
                offset += length;
            }

            // shutdown thread pool
            executor.shutdown();

            // wait for threads to finish and gather parts (future.get() will throw an exception if one occurred during execution)
            SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
            for (Future<MultipartPartETag> future : futures) {
                parts.add(future.get());
            }

            int maxParts = 2;

            ListPartsRequest listPartsRequest = new ListPartsRequest(getTestBucket(), key, uploadId);
            listPartsRequest.setMaxParts(maxParts);
            ListPartsResult listPartsResult = null;
            List<MultipartPart> allParts = new ArrayList<MultipartPart>();
            do {
                if (listPartsResult != null) listPartsRequest.setMarker(listPartsResult.getNextPartNumberMarker());
                listPartsResult = client.listParts(listPartsRequest);
                allParts.addAll(listPartsResult.getParts());
                Assert.assertEquals(2, listPartsResult.getParts().size());
                Assert.assertEquals(2, listPartsResult.getMaxParts().intValue());
            } while (listPartsResult.isTruncated());

            // verify the right number of parts is returned altogether
            Assert.assertEquals(file.length() / partSize + 1, allParts.size());

            // complete MP upload
            client.completeMultipartUpload(new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId).withParts(parts));
        } catch (Exception e) {
            client.abortMultipartUpload(new AbortMultipartUploadRequest(getTestBucket(), key, uploadId));
        }
    }

    @Test
    public void testMultiThreadMultipartUploadMostSimple() throws Exception {
        String key = "TestObject_" + UUID.randomUUID();
        int fiveKB = 5 * 1024;
        int partCnt = 7;
        byte[] b;
        InputStream tmpIs;
        List<InputStream> uploadPartsBytesList = new ArrayList<InputStream>();
        for (int i=0; i<partCnt;i++) {
            b = new byte[fiveKB];
            new Random().nextBytes(b);
            tmpIs = new ByteArrayInputStream(b, 0, fiveKB);
            uploadPartsBytesList.add(tmpIs);
        }

        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        List<Future<?>> futures = new ArrayList<Future<?>>();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        final AtomicInteger successCount = new AtomicInteger();
        int uploadPartNumber = 1;
        for(InputStream uploadPartStream: uploadPartsBytesList) {
            final UploadPartRequest request =
                    new UploadPartRequest(getTestBucket(), key, uploadId, uploadPartNumber, uploadPartStream);

            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    client.uploadPart(request);
                    successCount.incrementAndGet();
                }
            }));
            uploadPartNumber++;
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        Assert.assertEquals("at least one thread failed", futures.size(), successCount.intValue());

        ListPartsResult lpr = client.listParts(getTestBucket(), key, uploadId);
        List<MultipartPart> mpp = lpr.getParts();
        Assert.assertEquals("at least one part failed according to listParts", successCount.intValue(), mpp.size());

        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        MultipartPartETag eTag;
        for (MultipartPart part: mpp) {
            eTag = new MultipartPartETag(part.getPartNumber(), part.getETag());
            parts.add(eTag);
        }
        completionRequest.setParts(parts);
        client.completeMultipartUpload(completionRequest);
    }

    @Test
    public void testSingleMultipartUploadMostSimple() throws Exception {
        String key = "TestObject_" + UUID.randomUUID();
        int fiveKB = 5 * 1024;
        byte[] content1 = new byte[fiveKB];
        byte[] content2 = new byte[fiveKB];
        byte[] content3 = new byte[fiveKB];
        new Random().nextBytes(content1);
        new Random().nextBytes(content2);
        new Random().nextBytes(content3);
        InputStream is1 = new ByteArrayInputStream(content1, 0, fiveKB);
        InputStream is2 = new ByteArrayInputStream(content2, 0, fiveKB);
        InputStream is3 = new ByteArrayInputStream(content3, 0, fiveKB);

        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        MultipartPartETag mp1 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1, is1));

        MultipartPartETag mp2 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 2, is2));

        MultipartPartETag mp3 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 3, is3));

        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        parts.add(mp2);
        parts.add(mp3);

        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        client.completeMultipartUpload(completionRequest);
    }

    @Test
    public void testSingleMultipartUploadSimple() throws Exception {
        String key = "testMpuSimple";
        int fiveMB = 5 * 1024 * 1024;
        byte[] content = new byte[11 * 1024 * 1024];
        new Random().nextBytes(content);
        InputStream is1 = new ByteArrayInputStream(content, 0, fiveMB);
        InputStream is2 = new ByteArrayInputStream(content, fiveMB, fiveMB);
        InputStream is3 = new ByteArrayInputStream(content, 2 * fiveMB, content.length - (2 * fiveMB));

        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        MultipartPartETag mp1 = client.uploadPart(
                new UploadPartRequest(getTestBucket(), key, uploadId, 1, is1).withContentLength((long) fiveMB));
        MultipartPartETag mp2 = client.uploadPart(
                new UploadPartRequest(getTestBucket(), key, uploadId, 2, is2).withContentLength((long) fiveMB));
        MultipartPartETag mp3 = client.uploadPart(
                new UploadPartRequest(getTestBucket(), key, uploadId, 3, is3)
                        .withContentLength((long) content.length - (2 * fiveMB)));

        TreeSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.addAll(Arrays.asList(mp1, mp2, mp3));

        client.completeMultipartUpload(new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId).withParts(parts));
    }

    @Test
    public void testPutObject() throws Exception {
        String key = "objectKey";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }


    @Test
    public void testPutObjectWithSpace() throws Exception {
        String key = "This Has a Space.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }

    @Test
    public void testPutObjectWithPlus() throws Exception {
        String key = "This+Has+a+Plus.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }

    @Test
    public void testPutObjectWithPercent() throws Exception {
        String key = "This is 100% or something.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }

    @Test
    public void testPutObjectWithChinese() throws Exception {
        String key = "解析依頼C1B068.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }


    @Test
    public void testPutObjectWithSmartQuote() throws Exception {
        String key = "This is an ‘object’.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }

    /**
     * Tests all the items in the java.net.URI "punct" character class.
     */
    @Test
    public void testPutObjectWithUriPunct() throws Exception {
        String key = "URI punct characters ,;:$&+=.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }

    /**
     * Tests all the items in the java.net.URI "reserved" character class.
     */
    @Test
    public void testPutObjectWithUriReserved() throws Exception {
        String key = "URI reserved characters ?/[]@.txt";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "Object Content");
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
    }
    @Test
    public void testAppendObject() throws Exception {
        String key = "appendTest";
        String content = "Hello";

        client.putObject(getTestBucket(), key, content, "text/plain");

        String appendContent = " World!";
        long offset = client.appendObject(getTestBucket(), key, appendContent);

        Assert.assertEquals(content + appendContent, client.readObject(getTestBucket(), key, String.class));
        Assert.assertEquals(content.length(), offset);
    }

    @Test
    public void testCopyObject() throws Exception {
        String key1 = "source-object";
        String key2 = "copied-object";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectPlusSource() throws Exception {
        String key1 = "source+object+plus";
        String key2 = "copied-object-plus";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectPlusDest() throws Exception {
        String key1 = "source-object-plus-dest";
        String key2 = "copied+object+plus+dest";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }


    @Test
    public void testCopyObjectPlusBoth() throws Exception {
        String key1 = "source+object+plus+both";
        String key2 = "copied+object+plus+both";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectSpaceSrc() throws Exception {
        String key1 = "source object space src";
        String key2 = "copied-object-space-src";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectSpaceDest() throws Exception {
        String key1 = "source-object-space-dest";
        String key2 = "copied object space dest";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectSpaceBoth() throws Exception {
        String key1 = "source object space both";
        String key2 = "copied object space both";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectChineseSrc() throws Exception {
        String key1 = "prefix/source-object-服务器-src";
        String key2 = "prefix/copied object chinese src";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectChineseDest() throws Exception {
        String key1 = "prefix/source-object-chinese-dest";
        String key2 = "prefix/copied object 服务器 dest";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectChineseBoth() throws Exception {
        String key1 = "source-object-服务器-both";
        String key2 = "copied object 服务器 both";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key1, content, null);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));

        client.copyObject(getTestBucket(), key1, getTestBucket(), key2);
        Assert.assertEquals(content, client.readObject(getTestBucket(), key1, String.class));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key2, String.class));
    }

    @Test
    public void testCopyObjectSelf() throws Exception {
        String key = "object";
        String content = "Hello Copy!";

        client.putObject(getTestBucket(), key, content, null);
        GetObjectResult<String> result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);
        Assert.assertEquals(content, result.getObject());
        Date originalModified = result.getObjectMetadata().getLastModified();

        // wait a tick so mtime is different
        Thread.sleep(1000);

        client.copyObject(getTestBucket(), key, getTestBucket(), key);
        result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);
        Assert.assertEquals(content, result.getObject());
        Assert.assertTrue("modified date has not changed", result.getObjectMetadata().getLastModified().after(originalModified));
    }

    @Ignore // TODO: blocked by STORAGE-374
    @Test
    public void testCopyObjectWithMeta() throws Exception {
        String key1 = "object1", key2 = "object2", key3 = "object3";
        String content = "Hello copy with meta!";
        String ct = "text/plain", cc, cd, ce;
        cc = cd = ce = "none";
        Calendar expires = Calendar.getInstance();
        expires.setTimeInMillis(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        expires.set(Calendar.MILLISECOND, 0);
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("meta1", "value1");
        userMeta.put("meta2", "value2");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().withContentType(ct);
        objectMetadata.withCacheControl(cc).withContentDisposition(cd).withContentEncoding(ce);
        objectMetadata.withHttpExpires(expires.getTime());
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key1, content).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key1);
        Assert.assertEquals(ct, objectMetadata.getContentType());
        Assert.assertEquals(cc, objectMetadata.getCacheControl());
        Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());

        // test copy meta
        client.copyObject(new CopyObjectRequest(getTestBucket(), key1, getTestBucket(), key2));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key2);
        Assert.assertEquals(ct, objectMetadata.getContentType());
        Assert.assertEquals(cc, objectMetadata.getCacheControl());
        Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());

        // test replace meta
        ct = "application/octet-stream";
        cc = cd = ce = "new";
        expires.add(Calendar.DATE, 1);
        userMeta.clear();
        userMeta.put("meta3", "value3");
        userMeta.put("meta4", "value4");
        objectMetadata = new S3ObjectMetadata().withContentType(ct);
        objectMetadata.withCacheControl(cc).withContentDisposition(cd).withContentEncoding(ce);
        objectMetadata.withHttpExpires(expires.getTime());
        objectMetadata.setUserMetadata(userMeta);
        client.copyObject(new CopyObjectRequest(getTestBucket(), key1, getTestBucket(), key3).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key3);
        Assert.assertEquals(ct, objectMetadata.getContentType());
        Assert.assertEquals(cc, objectMetadata.getCacheControl());
        Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());
    }

    @Ignore // TODO: blocked by STORAGE-374
    @Test
    public void testUpdateMetadata() throws Exception {
        String key = "update-metadata";
        String content = "Hello update meta!";
        String ct = "text/plain", cc, cd, ce;
        cc = cd = ce = "none";
        Calendar expires = Calendar.getInstance();
        expires.add(Calendar.DATE, 1);
        expires.set(Calendar.MILLISECOND, 0);
        Map<String, String> userMeta = new HashMap<String, String>();
        userMeta.put("meta1", "value1");
        userMeta.put("meta2", "value2");

        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().withContentType(ct);
        objectMetadata.withCacheControl(cc).withContentDisposition(cd).withContentEncoding(ce);
        objectMetadata.withHttpExpires(expires.getTime());
        objectMetadata.setUserMetadata(userMeta);
        client.putObject(new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(ct, objectMetadata.getContentType());
        Assert.assertEquals(cc, objectMetadata.getCacheControl());
        Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());

        // test update meta
        ct = "application/octet-stream";
        cc = cd = ce = "new";
        expires.add(Calendar.DATE, 1);
        userMeta.clear();
        userMeta.put("meta3", "value3");
        userMeta.put("meta4", "value4");
        objectMetadata = new S3ObjectMetadata().withContentType(ct);
        objectMetadata.withCacheControl(cc).withContentDisposition(cd).withContentEncoding(ce);
        objectMetadata.withHttpExpires(expires.getTime());
        objectMetadata.setUserMetadata(userMeta);
        client.copyObject(new CopyObjectRequest(getTestBucket(), key, getTestBucket(), key).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(ct, objectMetadata.getContentType());
        Assert.assertEquals(cc, objectMetadata.getCacheControl());
        Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());
    }

    @Test
    public void testVerifyRead() throws Exception {
        String key = "objectKey";
        String content = "Hello Object!";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);

        String readContent = client.readObject(getTestBucket(), key, String.class);
        Assert.assertEquals("Wring object content", content, readContent);
    }

    @Test
    public void testStreamObjectBetweenBuckets() throws Exception {
        Random random = new Random();
        String bucket2 = getTestBucket() + "-B", bucket3 = getTestBucket() + "-C";
        String key1 = "below-32k.obj", key2 = "above-32k.obj";
        long size1 = 32 * 1024 - 1; // 1 less than 32k
        long size2 = 64 * 1024 + 1; // 1 more than 64k
        client.createBucket(bucket2);
        client.createBucket(bucket3);
        try {
            // write to first bucket
            byte[] data = new byte[(int) size1];
            random.nextBytes(data);
            client.putObject(getTestBucket(), key1, data, null);

            data = new byte[(int) size2];
            client.putObject(getTestBucket(), key2, data, null);

            // read then write to second bucket
            data = client.readObject(getTestBucket(), key1, byte[].class);
            client.putObject(bucket2, key1, data, null);
            Assert.assertEquals(data.length, client.readObject(bucket2, key1, byte[].class).length);

            data = client.readObject(getTestBucket(), key2, byte[].class);
            client.putObject(bucket2, key2, data, null);
            Assert.assertEquals(data.length, client.readObject(bucket2, key2, byte[].class).length);

            // stream to third bucket
            InputStream inputStream = client.readObjectStream(getTestBucket(), key1, null);
            PutObjectRequest request = new PutObjectRequest(bucket3, key1, inputStream);
            request.setObjectMetadata(new S3ObjectMetadata().withContentLength(size1));
            client.putObject(request);
            Assert.assertEquals(size1, client.readObject(bucket3, key1, byte[].class).length);

            inputStream = client.readObjectStream(getTestBucket(), key2, null);
            request = new PutObjectRequest(bucket3, key2, inputStream);
            request.setObjectMetadata(new S3ObjectMetadata().withContentLength(size2));
            client.putObject(request);
            Assert.assertEquals(size2, client.readObject(bucket3, key2, byte[].class).length);
        } finally {
            cleanUpBucket(bucket2);
            cleanUpBucket(bucket3);
        }
    }
  
    @Test
    public void testReadObjectStreamRange() throws Exception {
        String key = "objectKey";
        String content = "Object Content";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);
        l4j.debug("JMC - successfully created the test object. will read object");

        Range range = new Range((long) 0, (long) (content.length() / 2));
        InputStream is = client.readObjectStream(getTestBucket(), key, range);
        l4j.debug("JMC - readObjectStream seemed to succeed. Will confirm the object contest");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            l4j.debug("JMC LINE:" + line);
        }
        l4j.debug("JMC - Success");
    }

    //<T> GetObjectResult<T> getObject(GetObjectRequest request, Class<T> objectType);
    @Test
    public void testGetObjectResultTemplate() throws Exception {
        //creates objects named TestObject_ + zero based index
        this.createTestObjects("", 1);
        GetObjectRequest request = new GetObjectRequest(getTestBucket(),"TestObject_0");
        GetObjectResult<String> result = client.getObject(request, String.class);
        l4j.debug("JMC returned from client.getObject");
        l4j.debug("JMC getObject = " + result.getObject());
        S3ObjectMetadata meta = result.getObjectMetadata();
        l4j.debug("JMC meta.getContentLength(): " + meta.getContentLength());
    }

    @Test
    public void testBucketVersions() throws Exception {
        VersioningConfiguration config = new VersioningConfiguration();
        config.setStatus(VersioningConfiguration.Status.Enabled);
        client.setBucketVersioning(getTestBucket(), config);

        String key = "versionTest";
        String content1 = "content1", content2 = "content2";

        // create object
        client.putObject(getTestBucket(), key, content1, "text/plain");

        // update object (will create version)
        client.putObject(getTestBucket(), key, content2, "text/plain");

        Assert.assertEquals(content2, client.readObject(getTestBucket(), key, String.class));

        // list versions
        List<AbstractVersion> versions = client.listVersions(getTestBucket(), null).getVersions();
        Assert.assertEquals(2, versions.size());
        String firstVersionId = null;
        for (AbstractVersion version : versions) {
            Assert.assertTrue(version instanceof Version);
            Assert.assertEquals(key, version.getKey());
            Assert.assertNotNull(version.getVersionId());
            Assert.assertNotNull(version.getLastModified());
            Assert.assertNotNull(version.getOwner());
            Assert.assertEquals(content1.length(), (long) ((Version) version).getSize());
            Assert.assertNotNull(((Version) version).getETag());
            Assert.assertNotNull(((Version) version).getStorageClass());
            if (version.isLatest()) {
                Assert.assertEquals(content2, client.readObject(getTestBucket(), key, version.getVersionId(), String.class));
            } else {
                Assert.assertEquals(content1, client.readObject(getTestBucket(), key, version.getVersionId(), String.class));
                firstVersionId = version.getVersionId();
            }
        }

        // restore version (copy old version)
        client.copyObject(new CopyObjectRequest(getTestBucket(), key, getTestBucket(), key).withSourceVersionId(firstVersionId));
        Assert.assertEquals(content1, client.readObject(getTestBucket(), key, String.class));

        versions = client.listVersions(getTestBucket(), null).getVersions();
        Assert.assertEquals(3, versions.size());
        String thirdVersionId = null;
        for (AbstractVersion version : versions) {
            if (version.isLatest()) thirdVersionId = version.getVersionId();
        }

        // delete object (creates a delete marker)
        client.deleteObject(getTestBucket(), key);

        versions = client.listVersions(getTestBucket(), null).getVersions();
        Assert.assertEquals(4, versions.size());
        String fourthVersionId = null;
        for (AbstractVersion version : versions) {
            if (version.isLatest()) fourthVersionId = version.getVersionId();
        }

        // delete explicit versions, which should revert back to prior version
        client.deleteVersion(getTestBucket(), key, fourthVersionId);

        versions = client.listVersions(getTestBucket(), null).getVersions();
        Assert.assertEquals(3, versions.size());
        for (AbstractVersion version : versions) {
            if (version.isLatest()) Assert.assertEquals(thirdVersionId, version.getVersionId());
        }
        Assert.assertEquals(content1, client.readObject(getTestBucket(), key, String.class));

        client.deleteVersion(getTestBucket(), key, thirdVersionId);

        Assert.assertEquals(2, client.listVersions(getTestBucket(), null).getVersions().size());
        Assert.assertEquals(content2, client.readObject(getTestBucket(), key, String.class));
    }
    
    @Test
    public void testDeleteObjectsRequest() throws Exception {
        //int numObjects = 5;
        //this.createTestObjects(getTestBucket(), "delObjPrefex", numObjects);

        String content = "Object Content";
        String testObject1 = "/objectPrefix/testObject1";
        String testObject2 = "/objectPrefix/testObject2";
        client.putObject(getTestBucket(), testObject1, content, "text/plain");
        client.putObject(getTestBucket(), testObject2, content, "text/plain");
  
        DeleteObjectsRequest request = new DeleteObjectsRequest(getTestBucket())
            .withKeys(testObject1, testObject2);      
        DeleteObjectsResult results = client.deleteObjects(request);
        List<AbstractDeleteResult> resultList = results.getResults();
        Assert.assertEquals(2, resultList.size());
        for(AbstractDeleteResult result: resultList) {
            if (result instanceof DeleteError ) {
                this.inspectDeleteError((DeleteError)result);
            }
            else {
                this.inspectDeleteSuccess((DeleteSuccess)result);
            }
        }
    } 
    protected void inspectDeleteError(DeleteError deleteResult) {
        Assert.assertNotNull(deleteResult);
    }
    protected void inspectDeleteSuccess(DeleteSuccess deleteResult) {
        Assert.assertNotNull(deleteResult);
    }

    @Test
    public void testGetObjectMetadata() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(getTestBucket(), testObject);
        this.validateMetadataValues(objectMetadata);
    }


    @Test
    public void testGetObjectMetadataNoExist() throws Exception {
        String testObject = "/objectPrefix/noExist.txt";

        try {
            client.getObjectMetadata(getTestBucket(), testObject);
        } catch(S3Exception e) {
            Assert.assertEquals("Wrong HTTP status", 404, e.getHttpCode());
            Assert.assertEquals("Wrong ErrorCode", "NoSuchKey", e.getErrorCode());

            // Should not chain a SAX error
            Assert.assertNull("Should not be chained exception", e.getCause());
        }
    }
    
    @Test
    public void testGetObjectMetadataRequest() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(getTestBucket(), testObject);
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(request);
        this.validateMetadataValues(objectMetadata);
    }
    
    protected void validateMetadataValues(S3ObjectMetadata objectMetadata) throws Exception {
        Assert.assertNotNull(objectMetadata);
    }

    @Test
    public void testGetObjectAcl() throws Exception {
        String key = "getAclTest";
        client.putObject(getTestBucket(), key, "Hello ACLs!", "text/plain");

        AccessControlList acl = client.getObjectAcl(getTestBucket(), key);
        Assert.assertNotNull(acl.getOwner());
        Assert.assertNotNull(acl.getGrants());
        Assert.assertTrue(acl.getGrants().size() > 0);
        for (Grant grant : acl.getGrants()) {
            Assert.assertNotNull(grant.getGrantee());
            Assert.assertNotNull(grant.getPermission());
        }
    }

    @Test
    public void testGetObjectVersionAcl() throws Exception {
        // enable versioning on the bucket
        client.setBucketVersioning(getTestBucket(), new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));

        String key = "getVersionAclTest";
        client.putObject(getTestBucket(), key, "Hello Version ACLs!", "text/plain");

        String versionId = client.listVersions(getTestBucket(), null).getVersions().get(0).getVersionId();

        AccessControlList acl = client.getObjectAcl(new GetObjectAclRequest(getTestBucket(), key).withVersionId(versionId));
        Assert.assertNotNull(acl.getOwner());
        Assert.assertNotNull(acl.getGrants());
        Assert.assertTrue(acl.getGrants().size() > 0);
        for (Grant grant : acl.getGrants()) {
            Assert.assertNotNull(grant.getGrantee());
            Assert.assertNotNull(grant.getPermission());
        }
    }

    @Test
    public void testSetObjectAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        client.putObject(getTestBucket(), testObject, "Hello ACLs!", "text/plain");

        String identity = createS3Config().getIdentity();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setObjectAcl(getTestBucket(), testObject, acl);
        assertAclEquals(acl, client.getBucketAcl(getTestBucket()));
    }
    
    @Test
    public void testSetObjectCannedAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        client.setObjectAcl(getTestBucket(), testObject, CannedAcl.BucketOwnerFullControl);
        //TODO - need to validate this against a real acl
    }
    
    @Test
    public void testSetObjectAclRequestAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");

        String identity = createS3Config().getIdentity();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        SetObjectAclRequest request = new SetObjectAclRequest(getTestBucket(), testObject);
        l4j.debug("JMC calling request.setAcl");
        request.setAcl(acl);
        client.setObjectAcl(request);

        assertAclEquals(acl, client.getObjectAcl(getTestBucket(), testObject));
    }

    @Test
    public void testSetObjectAclRequestCanned() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        SetObjectAclRequest request = new SetObjectAclRequest(getTestBucket(), testObject);
        request.setCannedAcl(CannedAcl.BucketOwnerFullControl);
        client.setObjectAcl(request);
        //TODO - need to verify the returned acl is comparable to the canned acl
    }

    @Test
    public void testPreSignedUrl() throws Exception {
        S3Client tempClient = new S3JerseyClient(new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));
        URL url = tempClient.getPresignedUrl("johnsmith", "photos/puppy.jpg", new Date(1175139620000L));
        Assert.assertEquals("https://johnsmith.s3.amazonaws.com/photos/puppy.jpg" +
                "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1175139620&Signature=NpgCjnDzrM%2BWFzoENXmpNDUsSn8%3D",
                url.toString());
    }

    protected void assertAclEquals(AccessControlList acl1, AccessControlList acl2) {
        Assert.assertEquals(acl1.getOwner(), acl2.getOwner());
        Assert.assertEquals(acl1.getGrants(), acl2.getGrants());
    }
}