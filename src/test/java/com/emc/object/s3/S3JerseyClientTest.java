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

import com.emc.object.Method;
import com.emc.object.ObjectConfig;
import com.emc.object.Protocol;
import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.bean.BucketPolicyStatement.Effect;
import com.emc.object.s3.jersey.FaultInjectionFilter;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import com.emc.object.util.RestUtil;
import com.emc.object.util.TestProperties;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.ecs.Vdc;
import com.emc.rest.smart.ecs.VdcHost;
import com.emc.util.RandomInputStream;
import com.emc.util.TestConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class S3JerseyClientTest extends AbstractS3ClientTest {
    private static final Logger log = LoggerFactory.getLogger(S3JerseyClientTest.class);
    protected boolean testIAM = false;

    @Before
    public void checkIamUser() {
        try {
            Properties props = TestConfig.getProperties();
            testIAM = Boolean.parseBoolean(props.getProperty(TestProperties.S3_IAM_USER));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected String getTestBucketPrefix() {
        return "s3-client-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config());
    }

    @Test
    public void testListDataNodes() {
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

    @Ignore // for now since ECS always returns a 201 (AWS returns a 409 outside of the standard US region)
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
        client.createBucket(new CreateBucketRequest(bucketName));

        Assert.assertTrue(client.bucketExists(bucketName));
        client.deleteBucket(bucketName);
    }

    @Test
    public void testCreateFilesystemBucket() {
        Assume.assumeFalse("FS buckets are not supported with IAM user.", testIAM);

        String bucketName = getTestBucket() + "-y";

        client.createBucket(new CreateBucketRequest(bucketName).withFileSystemEnabled(true));

        client.deleteBucket(bucketName);
    }

    @Test
    public void testCreateStaleReadAllowedBucket() {
        String bucketName = getTestBucket() + "-z";

        client.createBucket(new CreateBucketRequest(bucketName).withStaleReadAllowed(true));

        client.deleteBucket(bucketName);
    }

    @Test
    public void testCreateEncryptedBucket() {
        String bucketName = getTestBucket() + "-enc";

        client.createBucket(new CreateBucketRequest(bucketName).withEncryptionEnabled(true));

        client.deleteBucket(bucketName);
    }

    @Test
    public void testEnableObjectLockOnExistingBucket() {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        ObjectLockConfiguration objectLockConfig = client.getObjectLockConfiguration(bucketName);
        Assert.assertNull(objectLockConfig);
        client.enableObjectLock(bucketName);
        objectLockConfig = client.getObjectLockConfiguration(bucketName);
        Assert.assertEquals(ObjectLockConfiguration.ObjectLockEnabled.Enabled, objectLockConfig.getObjectLockEnabled());
    }

    @Test
    public void testCreateObjectLockBucket() {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = "s3-client-test-createObjectLockBucket";
        client.createBucket(new CreateBucketRequest(bucketName).withObjectLockEnabled(true));
        ObjectLockConfiguration objectLockConfig = client.getObjectLockConfiguration(bucketName);
        Assert.assertEquals(ObjectLockConfiguration.ObjectLockEnabled.Enabled, objectLockConfig.getObjectLockEnabled());
        client.deleteBucket(bucketName);
    }

    @Test
    public void testSetObjectLockConfiguration() {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        ObjectLockConfiguration objectLockConfig = new ObjectLockConfiguration().withObjectLockEnabled(ObjectLockConfiguration.ObjectLockEnabled.Enabled);
        DefaultRetention defaultRetention = new DefaultRetention().withMode(ObjectLockRetentionMode.GOVERNANCE).withDays(2);
        objectLockConfig.setRule(new ObjectLockRule().withDefaultRetention(defaultRetention));
        try {
            client.setObjectLockConfiguration(bucketName, objectLockConfig);
            Assert.fail("Exception is expected when setting Object Lock configuration on existing bucket without ObjectLock being enabled.");
        } catch (S3Exception e) {
            Assert.assertEquals(409, e.getHttpCode());
            Assert.assertEquals("InvalidBucketState", e.getErrorCode());
        }

        client.enableObjectLock(bucketName);
        client.setObjectLockConfiguration(bucketName, objectLockConfig);
        ObjectLockConfiguration objectLockConfig_verify = client.getObjectLockConfiguration(bucketName);

        Assert.assertEquals(objectLockConfig.getObjectLockEnabled(), objectLockConfig_verify.getObjectLockEnabled());
        Assert.assertEquals(defaultRetention.getMode(), objectLockConfig_verify.getRule().getDefaultRetention().getMode());
        Assert.assertEquals(defaultRetention.getDays(), objectLockConfig_verify.getRule().getDefaultRetention().getDays());
        Assert.assertEquals(defaultRetention.getYears(), objectLockConfig_verify.getRule().getDefaultRetention().getYears());
    }

    @Test
    public void testDeleteObjectWithLegalHoldNotAllowed() throws Exception {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        String key = "testObject_DeleteWithLegalHold";
        client.enableObjectLock(bucketName);
        ObjectLockLegalHold objectLockLegalHold = new ObjectLockLegalHold().withStatus(ObjectLockLegalHold.Status.ON);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, "test Delete With LegalHold Not Allowed")
                .withObjectMetadata(new S3ObjectMetadata().withObjectLockLegalHold(objectLockLegalHold));
        client.putObject(putObjectRequest);
        String versionId = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();

        Assert.assertEquals(ObjectLockLegalHold.Status.ON, client.getObjectLegalHold(new GetObjectLegalHoldRequest(bucketName, key).withVersionId(versionId)).getStatus());
        try {
            client.deleteVersion(bucketName, key, versionId);
            Assert.fail("Exception is expected when deleting version objects with Legal Hold ON.");
        } catch (S3Exception e) {
            Assert.assertEquals("AccessDenied", e.getErrorCode());
        } finally {
            objectLockLegalHold.setStatus(ObjectLockLegalHold.Status.OFF);
            client.setObjectLegalHold(new SetObjectLegalHoldRequest(bucketName, key).withVersionId(versionId).withLegalHold(objectLockLegalHold));
            client.deleteVersion(bucketName, key, versionId);
        }
    }

    @Test
    public void testPutObjectLegalHold() throws Exception {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        String key = "testObject_PutObjectLegalHold";
        client.enableObjectLock(bucketName);

        //Put Legal Hold on create
        ObjectLockLegalHold objectLockLegalHold = new ObjectLockLegalHold().withStatus(ObjectLockLegalHold.Status.ON);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, "test Put Object LegalHold")
                .withObjectMetadata(new S3ObjectMetadata().withObjectLockLegalHold(objectLockLegalHold));
        client.putObject(putObjectRequest);
        String versionId = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();
        GetObjectLegalHoldRequest getObjectLegalHoldRequest = new GetObjectLegalHoldRequest(bucketName, key).withVersionId(versionId);
        Assert.assertEquals(ObjectLockLegalHold.Status.ON, client.getObjectLegalHold(getObjectLegalHoldRequest).getStatus());
        Assert.assertEquals(ObjectLockLegalHold.Status.ON, client.getObjectMetadata(bucketName, key).getObjectLockLegalHold().getStatus());

        //Put Legal Hold on existing object
        objectLockLegalHold.setStatus(ObjectLockLegalHold.Status.OFF);
        SetObjectLegalHoldRequest request = new SetObjectLegalHoldRequest(bucketName, key).withVersionId(versionId).withLegalHold(objectLockLegalHold);
        client.setObjectLegalHold(request);
        Assert.assertEquals(ObjectLockLegalHold.Status.OFF, client.getObjectLegalHold(getObjectLegalHoldRequest).getStatus());
        Assert.assertEquals(ObjectLockLegalHold.Status.OFF, client.getObjectMetadata(bucketName, key).getObjectLockLegalHold().getStatus());
    }

    @Test
    public void testPutObjectRetention() throws Exception {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        String key = "testObject_PutObjectRetention";
        client.enableObjectLock(bucketName);
        Date retentionDate = new Date(System.currentTimeMillis() + 2000);
        ObjectLockRetention objectLockRetention = new ObjectLockRetention()
                .withMode(ObjectLockRetentionMode.COMPLIANCE)
                .withRetainUntilDate(retentionDate);
        //Put Retention on Create
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, "test Put Object Retention")
                .withObjectMetadata(new S3ObjectMetadata().withObjectLockRetention(objectLockRetention));
        client.putObject(putObjectRequest);
        String versionId = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();
        GetObjectRetentionRequest request = new GetObjectRetentionRequest(bucketName, key).withVersionId(versionId);
        ObjectLockRetention objectLockRetention2 = client.getObjectRetention(request);
        Assert.assertEquals(objectLockRetention.getMode(), objectLockRetention2.getMode());
        Assert.assertEquals(retentionDate, objectLockRetention2.getRetainUntilDate());
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(bucketName, key);
        Assert.assertEquals(objectLockRetention.getMode(), objectMetadata.getObjectLockRetention().getMode());
        Assert.assertEquals(retentionDate, objectMetadata.getObjectLockRetention().getRetainUntilDate());
        Thread.sleep(2000);

        //Put Retention on existing object.
        Date retentionDate2 = new Date(System.currentTimeMillis() + 2000);
        objectLockRetention.setRetainUntilDate(retentionDate2);
        objectLockRetention.setMode(ObjectLockRetentionMode.GOVERNANCE);
        SetObjectRetentionRequest setObjectRetentionRequest = new SetObjectRetentionRequest(bucketName, key).withVersionId(versionId)
                .withRetention(objectLockRetention);
        client.setObjectRetention(setObjectRetentionRequest);
        objectLockRetention2 = client.getObjectRetention(request);
        Assert.assertEquals(objectLockRetention.getMode(), objectLockRetention2.getMode());
        Assert.assertEquals(retentionDate2, objectLockRetention2.getRetainUntilDate());
        objectMetadata = client.getObjectMetadata(bucketName, key);
        Assert.assertEquals(objectLockRetention.getMode(), objectMetadata.getObjectLockRetention().getMode());
        Assert.assertEquals(retentionDate2, objectMetadata.getObjectLockRetention().getRetainUntilDate());
        Thread.sleep(2000);
    }

    @Test
    public void testDeleteObjectWithBypassGovernance() throws Exception {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        String key = "testDeleteObjectWithBypassGovernance";
        client.enableObjectLock(bucketName);
        Date retentionDate = new Date(System.currentTimeMillis() + 200000);
        ObjectLockRetention objectLockRetention = new ObjectLockRetention()
                .withMode(ObjectLockRetentionMode.GOVERNANCE)
                .withRetainUntilDate(retentionDate);

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, "test DeleteObjectWithBypassGovernance")
                .withObjectMetadata(new S3ObjectMetadata().withObjectLockRetention(objectLockRetention));
        client.putObject(putObjectRequest);
        String versionId = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();

        ObjectKey objectKey = new ObjectKey(key, versionId);
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName).withKeys(objectKey);

        //Expect failure without bypassGovernanceRetention (DeleteObjectsRequest)
        DeleteObjectsResult deleteObjectsResult = client.deleteObjects(deleteObjectsRequest);
        Assert.assertTrue(deleteObjectsResult.getResults().get(0) instanceof DeleteError);

        //Expect failure without bypassGovernanceRetention (DeleteObjectRequest)
        DeleteObjectRequest request = new DeleteObjectRequest(bucketName, key).withVersionId(versionId);
        try {
            client.deleteObject(request);
            Assert.fail("expected 403");
        } catch (S3Exception e) {
            Assert.assertEquals(403, e.getHttpCode());
        }

        //Expect success with bypassGovernanceRetention
        deleteObjectsRequest.setBypassGovernanceRetention(true);
        deleteObjectsResult = client.deleteObjects(deleteObjectsRequest);
        Assert.assertTrue(deleteObjectsResult.getResults().get(0) instanceof DeleteSuccess);
    }

    @Test
    public void testCopyObjectWithLegalHoldON() throws Exception {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        String key1 = "source-object";
        String key2 = "copied-object";
        String content = "Hello Copy!";

        client.putObject(bucketName, key1, content, null);
        Assert.assertEquals(content, client.readObject(bucketName, key1, String.class));

        client.enableObjectLock(bucketName);
        ObjectLockLegalHold objectLockLegalHold = new ObjectLockLegalHold().withStatus(ObjectLockLegalHold.Status.ON);
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, key1, bucketName, key2)
                .withObjectMetadata(new S3ObjectMetadata().withObjectLockLegalHold(objectLockLegalHold));
        client.copyObject(copyObjectRequest);
        Assert.assertEquals(content, client.readObject(bucketName, key2, String.class));
        String versionId = client.listVersions(bucketName, key2).getVersions().get(0).getVersionId();
        GetObjectLegalHoldRequest getObjectLegalHoldRequest = new GetObjectLegalHoldRequest(bucketName, key2).withVersionId(versionId);
        Assert.assertEquals(ObjectLockLegalHold.Status.ON, client.getObjectLegalHold(getObjectLegalHoldRequest).getStatus());

        objectLockLegalHold.setStatus(ObjectLockLegalHold.Status.OFF);
        SetObjectLegalHoldRequest setObjectLegalHoldRequest = new SetObjectLegalHoldRequest(bucketName, key2).withVersionId(versionId).withLegalHold(objectLockLegalHold);
        client.setObjectLegalHold(setObjectLegalHoldRequest);
    }

    @Test
    public void testSingleMultipartUploadWithRetention() throws Exception {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
        Assume.assumeTrue("Skip Object Lock related tests for non IAM user.", testIAM);

        String bucketName = getTestBucket();
        String key = "testMpuSimple";
        int fiveMB = 5 * 1024 * 1024;
        byte[] content = new byte[11 * 1024 * 1024];
        new Random().nextBytes(content);
        InputStream is1 = new ByteArrayInputStream(content, 0, fiveMB);
        InputStream is2 = new ByteArrayInputStream(content, fiveMB, fiveMB);
        Date retentionDate = new Date(System.currentTimeMillis() + 2000);
        ObjectLockRetention objectLockRetention = new ObjectLockRetention().withMode(ObjectLockRetentionMode.GOVERNANCE).withRetainUntilDate(retentionDate);

        client.enableObjectLock(bucketName);
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key)
                .withObjectMetadata(new S3ObjectMetadata().withObjectLockRetention(objectLockRetention));
        String uploadId = client.initiateMultipartUpload(request).getUploadId();
        MultipartPartETag mp1 = client.uploadPart(
                new UploadPartRequest(bucketName, key, uploadId, 1, is1).withContentLength((long) fiveMB));
        MultipartPartETag mp2 = client.uploadPart(
                new UploadPartRequest(bucketName, key, uploadId, 2, is2).withContentLength((long) fiveMB));
        TreeSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>(Arrays.asList(mp1, mp2));
        client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, key, uploadId).withParts(parts));

        String versionId = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();
        GetObjectRetentionRequest getObjectRetentionRequest = new GetObjectRetentionRequest(bucketName, key).withVersionId(versionId);
        ObjectLockRetention objectLockRetention2 = client.getObjectRetention(getObjectRetentionRequest);
        Assert.assertEquals(objectLockRetention.getMode(), objectLockRetention2.getMode());
        Assert.assertEquals(retentionDate, objectLockRetention2.getRetainUntilDate());
        Thread.sleep(2000);
    }

    @Test // also tests create-with-retention-period
    public void testGetBucketInfo() {
        String bucketName = getTestBucket() + "-u";
        long retentionPeriod = 3600; // 1 hour

        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.withRetentionPeriod(retentionPeriod);
        client.createBucket(request);

        try {
            BucketInfo info = client.getBucketInfo(bucketName);
            Assert.assertEquals(bucketName, info.getBucketName());
            Assert.assertEquals(new Long(retentionPeriod), info.getRetentionPeriod());
        } finally {
            client.deleteBucket(bucketName);
        }
    }

    @Test
    public void testDeleteBucket() throws Exception {
        Thread.sleep(1000); // discover all hosts

        String bucketName = getTestBucket() + "-x";
        Assert.assertFalse("bucket should not exist " + bucketName, client.bucketExists(bucketName));

        client.createBucket(bucketName);
        Assert.assertTrue("failed to create bucket " + bucketName, client.bucketExists(bucketName));

        // write and delete an object
        client.putObject(bucketName, "foo", "bar", null);
        client.deleteObject(bucketName, "foo");

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
        // need to get canonical user ID from bucket ACL (for IAM users, this is different from the access key)
        String identity = client.getBucketAcl(getTestBucket()).getOwner().getId();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setBucketAcl(getTestBucket(), acl);

        this.assertAclEquals(acl, client.getBucketAcl(getTestBucket()));
    }

    @Test
    public void testSetBucketAclCanned() throws Exception {
        // need to get canonical user ID from bucket ACL (for IAM users, this is different from the access key)
        String identity = client.getBucketAcl(getTestBucket()).getOwner().getId();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setBucketAcl(getTestBucket(), CannedAcl.Private);

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
    public void testBucketLifecycle() {
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 300);

        LifecycleConfiguration lc = new LifecycleConfiguration();
        lc.withRules(new LifecycleRule("expires-7-years", "", LifecycleRule.Status.Enabled)
                .withExpirationDays(7 * 365));

        client.setBucketLifecycle(getTestBucket(), lc);

        LifecycleConfiguration lc2 = client.getBucketLifecycle(getTestBucket());
        Assert.assertNotNull(lc2);
        Assert.assertEquals(lc.getRules().size(), lc2.getRules().size());

        for (LifecycleRule rule : lc.getRules()) {
            Assert.assertTrue(lc2.getRules().contains(rule));
        }

        lc.withRules(new LifecycleRule("archive-expires-180", "archive/", LifecycleRule.Status.Enabled)
                .withExpirationDays(180)
                .withNoncurrentVersionExpirationDays(50));

        client.setBucketLifecycle(getTestBucket(), lc);

        lc2 = client.getBucketLifecycle(getTestBucket());
        Assert.assertNotNull(lc2);
        Assert.assertEquals(lc.getRules().size(), lc2.getRules().size());

        for (LifecycleRule rule : lc.getRules()) {
            Assert.assertTrue(lc2.getRules().contains(rule));
        }

        lc.withRules(new LifecycleRule("armageddon", "", LifecycleRule.Status.Enabled).withExpirationDate(end.getTime()));

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

    @Test // Note: affected by STORAGE-22520
    public void testBucketPolicy() {
        BucketPolicy bucketPolicy = new BucketPolicy().withVersion("2012-10-17").withId("new-policy-1")
                .withStatements(
                        new BucketPolicyStatement()
                                .withSid("statement-1")
                                .withEffect(Effect.Allow)
                                .withPrincipal("*")
                                .withResource("arn:aws:s3:::" + getTestBucket() + "/*")
                                .withActions(BucketPolicyAction.DeleteObjectVersion, BucketPolicyAction.DeleteObject)
                                .withCondition(PolicyConditionOperator.StringEquals, new PolicyConditionCriteria()
                                        .withCondition(PolicyConditionKey.UserAgent, "foo-client"))
                );

        client.setBucketPolicy(getTestBucket(), bucketPolicy);

        Assert.assertEquals(bucketPolicy, client.getBucketPolicy(getTestBucket()));

        client.deleteBucketPolicy(getTestBucket());
        try {
            client.getBucketPolicy(getTestBucket());
            Assert.fail("get-policy should have thrown an exception after deleting policy");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchBucketPolicy", e.getErrorCode());
        }
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

    @Test // bug-ref: STORAGE-6791
    public void testListObjectsPagingWithEncodedDelim() {
        String prefix = "test\u001dDelim/", delim = "/", key = "foo\u001dbar", content = "Hello List Delim!";
        client.putObject(getTestBucket(), prefix + key + 1, content, null);
        client.putObject(getTestBucket(), prefix + key + 2, content, null);
        client.putObject(getTestBucket(), prefix + key + 3, content, null);
        client.putObject(getTestBucket(), prefix + key + 4, content, null);
        client.putObject(getTestBucket(), prefix + key + 5, content, null);

        ListObjectsResult result = client.listObjects(new ListObjectsRequest(getTestBucket()).withPrefix(prefix)
                .withDelimiter(delim).withMaxKeys(3).withEncodingType(EncodingType.url));
        Assert.assertNotNull("ListObjectsResult was null and should NOT be", result);
        Assert.assertEquals("The correct number of objects were NOT returned", 3, result.getObjects().size());
        Assert.assertTrue(result.isTruncated());
        Assert.assertEquals("/", result.getDelimiter());
        Assert.assertEquals(prefix, result.getPrefix());
        Assert.assertEquals(prefix + key + 1, result.getObjects().get(0).getKey());

        // get next page
        result = client.listMoreObjects(result);
        Assert.assertNotNull("ListObjectsResult was null and should NOT be", result);
        Assert.assertEquals("The correct number of objects were NOT returned", 2, result.getObjects().size());
        Assert.assertFalse(result.isTruncated());
        Assert.assertEquals("/", result.getDelimiter());
        Assert.assertEquals(prefix, result.getPrefix());
        Assert.assertEquals(prefix + key + 4, result.getObjects().get(0).getKey());
    }

    @Test
    public void testListObjectsPaging() {
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
    public void testListObjectsPagingDelim() {
        int numObjects = 10;

        this.createTestObjects("foo/", numObjects);

        List<S3Object> objects = new ArrayList<S3Object>();
        ListObjectsResult result = null;
        int requestCount = 0;
        do {
            if (result == null)
                result = client.listObjects(new ListObjectsRequest(getTestBucket()).withMaxKeys(3).withPrefix("foo/").withDelimiter("/"));
            else result = client.listMoreObjects(result);
            objects.addAll(result.getObjects());
            requestCount++;
        } while (result.isTruncated());

        Assert.assertEquals("The correct number of objects were NOT returned", numObjects, objects.size());
        Assert.assertEquals("should be 4 pages", 4, requestCount);
    }

    @Test
    public void testListObjectsPagingWithPrefix() {
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
    public void testListObjectsWithEncoding() {
        String key = "foo\u001do", content = "Hello List!";
        client.putObject(getTestBucket(), key, content, null);

        try {
            ListObjectsRequest request = new ListObjectsRequest(getTestBucket()).withEncodingType(EncodingType.url);
            ListObjectsResult result = client.listObjects(request);
            Assert.assertNotNull("ListObjectsResult was null, but should NOT have been", result);

            List<S3Object> resultObjects = result.getObjects();
            Assert.assertNotNull("List<S3Object> was null, but should NOT have been", resultObjects);
            Assert.assertEquals(1, resultObjects.size());

            S3Object object = resultObjects.get(0);
            Assert.assertEquals(key, object.getKey());

        } finally {
            client.deleteObject(getTestBucket(), key);
        }
    }

    @Test
    public void listObjectsWithPercentAndUrlEncoding() {
        String key1 = "f%o%%o", key2 = "fo%%o", key3 = "fo%o", content = "Hello List Percent!";
        client.putObject(getTestBucket(), key1, content, null);
        client.putObject(getTestBucket(), key2, content, null);
        client.putObject(getTestBucket(), key3, content, null);

        ListObjectsRequest request = new ListObjectsRequest(getTestBucket()).withEncodingType(EncodingType.url);
        ListObjectsResult result = client.listObjects(request);
        Assert.assertNotNull("ListObjectsResult was null, but should NOT have been", result);

        List<S3Object> resultObjects = result.getObjects();
        Assert.assertNotNull("List<S3Object> was null, but should NOT have been", resultObjects);
        Assert.assertEquals(3, resultObjects.size());

        Assert.assertEquals(key1, resultObjects.get(0).getKey());
        Assert.assertEquals(key2, resultObjects.get(1).getKey());
        Assert.assertEquals(key3, resultObjects.get(2).getKey());
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
    public void testListVersionsPaging() {
        // turn on versioning first
        client.setBucketVersioning(getTestBucket(),
                new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));

        // create a few versions of the same key
        String key = "prefix/foo", content = "Hello Version Paging!";
        client.putObject(getTestBucket(), key, content, null);
        client.deleteObject(getTestBucket(), key);
        client.putObject(getTestBucket(), key, content, null);

        // create key in sub-prefix
        key = "prefix/prefix2/bar";
        client.putObject(getTestBucket(), key, content, null);
        client.deleteObject(getTestBucket(), key);
        client.putObject(getTestBucket(), key, content, null);

        List<AbstractVersion> versions = new ArrayList<AbstractVersion>();
        ListVersionsResult result = null;
        int requestCount = 0;
        do {
            if (result == null) result = client.listVersions(new ListVersionsRequest(getTestBucket()).withMaxKeys(2));
            else result = client.listMoreVersions(result);
            versions.addAll(result.getVersions());
            requestCount++;
        } while (result.isTruncated());

        assertForListVersionsPaging(versions.size(), requestCount);
    }

    protected void assertForListVersionsPaging(int size, int requestCount) {
        Assert.assertEquals("The correct number of versions were NOT returned", 6, size);
        Assert.assertEquals("should be 3 pages", 3, requestCount);
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
        key = "prefix/prefix2/bar";
        client.putObject(getTestBucket(), key, content, null);
        client.deleteObject(getTestBucket(), key);
        client.putObject(getTestBucket(), key, content, null);

        ListVersionsRequest request = new ListVersionsRequest(getTestBucket()).withPrefix("prefix/")
                .withDelimiter("/").withMaxKeys(4);
        ListVersionsResult result = client.listVersions(request);

        Assert.assertEquals(3, result.getVersions().size());
        Assert.assertEquals(1, result.getCommonPrefixes().size());
        Assert.assertEquals("prefix/prefix2/", result.getCommonPrefixes().get(0));
        Assert.assertFalse(result.isTruncated());
    }

    protected void createTestObjects(String prefix, int numObjects) {
        if (prefix == null) prefix = "";

        byte[] content = new byte[5 * 1024];
        new Random().nextBytes(content);

        for (int i = 0; i < numObjects; i++) {
            client.putObject(getTestBucket(), prefix + "TestObject_" + i, content, null);
        }
    }

    @Test
    public void testReadObject() {
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
    public void testGetObjectPreconditions() {
        String key = "testGetPreconditions";
        String content = "hello GET preconditions!";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5); // 5 minutes ago

        client.putObject(getTestBucket(), key, content, "text/plain");
        String etag = client.getObjectMetadata(getTestBucket(), key).getETag();

        // test if-modified pass
        GetObjectRequest request = new GetObjectRequest(getTestBucket(), key);
        request.withIfModifiedSince(cal.getTime());
        Assert.assertNotNull(client.getObject(request, String.class));

        // test if-unmodified fail
        request.withIfModifiedSince(null).withIfUnmodifiedSince(cal.getTime());
        Assert.assertNull(client.getObject(request, String.class));

        // test if-modified fail
        cal.add(Calendar.MINUTE, 10); // 5 minutes from now
        request.withIfUnmodifiedSince(null).withIfModifiedSince(cal.getTime());
        Assert.assertNull(client.getObject(request, String.class));

        // test if-unmodified pass
        request.withIfModifiedSince(null).withIfUnmodifiedSince(cal.getTime());
        Assert.assertNotNull(client.getObject(request, String.class));

        // test if-match pass
        request.withIfUnmodifiedSince(null).withIfMatch(etag);
        Assert.assertNotNull(client.getObject(request, String.class));

        // test if-none-match fail
        request.withIfMatch(null).withIfNoneMatch(etag);
        Assert.assertNull(client.getObject(request, String.class));

        etag = "d41d8cd98f00b204e9800998ecf8427e";

        // test if-none-match pass
        request.withIfNoneMatch(etag);
        Assert.assertNotNull(client.getObject(request, String.class));

        // test if-match fail
        request.withIfNoneMatch(null).withIfMatch(etag);
        Assert.assertNull(client.getObject(request, String.class));
    }

    @Test // NOTE: affected by STORAGE-22521
    public void testPutObjectPreconditions() {
        String key = "testGetPreconditions";
        String content = "hello GET preconditions!";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5); // 5 minutes ago

        client.putObject(getTestBucket(), key, content, "text/plain");
        String etag = client.getObjectMetadata(getTestBucket(), key).getETag();

        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content);

        // test if-unmodified fail
        request.withIfUnmodifiedSince(cal.getTime());
        try {
            client.putObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-modified pass
        request.withIfUnmodifiedSince(null).withIfModifiedSince(cal.getTime());
        client.putObject(request);

        // test if-modified fail
        cal.add(Calendar.MINUTE, 10); // 5 minutes from now
        request.withIfModifiedSince(cal.getTime());
        try {
            client.putObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-unmodified pass
        request.withIfModifiedSince(null).withIfUnmodifiedSince(cal.getTime());
        client.putObject(request);

        // test if-match pass
        request.withIfUnmodifiedSince(null).withIfMatch(etag);
        client.putObject(request);

        // test if-none-match fail
        request.withIfMatch(null).withIfNoneMatch(etag);
        try {
            client.putObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        etag = "d41d8cd98f00b204e9800998ecf8427e";

        // test if-none-match pass
        request.withIfNoneMatch(etag);
        client.putObject(request);

        // test if-match fail
        request.withIfNoneMatch(null).withIfMatch(etag);
        try {
            client.putObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-match * (if key exists, i.e. update only) pass
        request.withIfNoneMatch(null).withIfMatch("*");
        client.putObject(request);

        // test if-none-match * (if key is new, i.e. create only) fail
        request.withIfMatch(null).withIfNoneMatch("*");
        try {
            client.putObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        request.setKey("bogus-key");

        // test if-match * fail
        request.withIfNoneMatch(null).withIfMatch("*");
        try {
            client.putObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-none-match * pass
        request.withIfMatch(null).withIfNoneMatch("*");
        client.putObject(request);
    }

    @Test
    public void testDeleteObjectPreconditions() {
        Assume.assumeTrue("ECS version must be at least 3.7.1", ecsVersion != null && ecsVersion.compareTo("3.7.1") >= 0);
        String key = "testDeletePreconditions";
        String content = "hello Delete preconditions!";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5); // 5 minutes ago

        client.putObject(getTestBucket(), key, content, "text/plain");
        String etag = client.getObjectMetadata(getTestBucket(), key).getETag();
        String etag2 = "d41d8cd98f00b204e9800998ecf8427e"; //non-matching Etag

        DeleteObjectRequest request = new DeleteObjectRequest(getTestBucket(), key);

        // test if-unmodified fail
        request.withIfUnmodifiedSince(cal.getTime());
        try {
            client.deleteObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-unmodified and if-match(correct etag) fail
        request.withIfUnmodifiedSince(cal.getTime()).withIfMatch(etag);
        try {
            client.deleteObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-unmodified pass
        cal.add(Calendar.MINUTE, 10); // 5 minutes from now
        request.withIfUnmodifiedSince(cal.getTime()).withIfMatch(null);
        client.deleteObject(request);

        client.putObject(getTestBucket(), key, content, "text/plain");
        // test if-unmodified and if-match(non-matching etag) fail
        request.withIfUnmodifiedSince(cal.getTime()).withIfMatch(etag2);
        try {
            client.deleteObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        // test if-match(non-matching etag) fail
        request.withIfUnmodifiedSince(null).withIfMatch(etag2);
        try {
            client.deleteObject(request);
            Assert.fail("expected 412");
        } catch (S3Exception e) {
            Assert.assertEquals(412, e.getHttpCode());
        }

        //test if-match(correct etag) pass
        request.withIfUnmodifiedSince(null).withIfMatch(etag);
        client.deleteObject(request);

        client.putObject(getTestBucket(), key, content, "text/plain");
        // test if-match * pass
        request.withIfUnmodifiedSince(null).withIfMatch("*");
        client.deleteObject(request);

        // test pre-condition should not fail on non-existing key
        request.setKey("bogus-key");
        cal.add(Calendar.MINUTE, -10); // 5 minutes ago
        request.withIfUnmodifiedSince(cal.getTime()).withIfMatch(etag2);
        client.deleteObject(request);
    }

    @Test
    public void testCreateObjectByteArray() {
        byte[] data;
        Random random = new Random();

        data = new byte[15];
        random.nextBytes(data);
        // FYI, this will set a content-length
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
    public void testCreateObjectWithStream() throws Exception {
        byte[] data = new byte[100];
        Random random = new Random();
        random.nextBytes(data);

        // FYI, this will set a content-length
        client.putObject(getTestBucket(), "byte-array-test", new ByteArrayInputStream(data), null);
        Assert.assertArrayEquals(data, client.readObject(getTestBucket(), "byte-array-test", byte[].class));

        // ... and this will use chunked-encoding
        client.putObject(getTestBucket(), "random-array-test", new RandomInputStream(100), null);
        Assert.assertEquals(new Long(100), client.getObjectMetadata(getTestBucket(), "random-array-test").getContentLength());
    }

    @Test
    public void testCreateJsonObjectWithStream() {
        String key = "json-stream-test";
        long size = 100;
        InputStream stream = new RandomInputStream(size);

        client.putObject(getTestBucket(), "json-stream-test", stream, "application/json");
        Assert.assertEquals(size, client.getObjectMetadata(getTestBucket(), key).getContentLength().longValue());
    }

    @Test
    public void testCreateObjectString() {
        String key = "string-test";
        String content = "Hello Strings!";
        client.putObject(getTestBucket(), key, content, "text/plain");
        Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testCreateObjectWithRequest() {
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), "/objectPrefix/testObject1", "object content");
        PutObjectResult result = client.putObject(request);
        Assert.assertNotNull(result);
    }

    @Test
    public void testCreateObjectChunkedWithRequest() {
        int size = 50000;
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        String dataStr = new String(data);
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), "/objectPrefix/testObject1", dataStr);

        //request.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        //request.property(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, -1);
        //request.property(ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING, Boolean.FALSE);

        PutObjectResult result = client.putObject(request);
        Assert.assertNotNull(result);
    }

    @Test
    public void testCreateObjectWithMetadata() {
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
    public void testServerSideEncryption() {
        String key = "object-with-serverside-encryption";
        String content = "Hello SSE-S3!";
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata().withServerSideEncryption(SseAlgorithm.AES256);
        client.putObject(new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals(SseAlgorithm.AES256, objectMetadata.getServerSideEncryption());
        Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testCreateObjectWithRetentionPeriod() throws Exception {
        String key = "object-in-retention";
        String content = "Hello Retention!";
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.setRetentionPeriod(4L);
        client.putObject(new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(objectMetadata));
        objectMetadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals((Long) 4L, objectMetadata.getRetentionPeriod());
        Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
        try {
            client.putObject(getTestBucket(), key, "evil update!", null);
            Assert.fail("object in retention allowed update");
        } catch (S3Exception e) {
            Assert.assertEquals("ObjectUnderRetention", e.getErrorCode());
        }

        Thread.sleep(10000); // allow retention to expire
        client.putObject(getTestBucket(), key, "good update!", null);
    }

    @Test
    public void testCreateObjectWithRetentionPolicy() throws Exception {
        String key = "object-in-retention-policy";
        String content = "Hello Retention Policy!";
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.setRetentionPolicy("bad-policy");
        client.putObject(new PutObjectRequest(getTestBucket(), key, content).withObjectMetadata(objectMetadata));

        // no way to verify, so if no error is returned, assume success
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
    public void testBucketLocation() throws Exception {
        LocationConstraint lc = client.getBucketLocation(getTestBucket());
        Assert.assertNotNull(lc);
        log.debug("Bucket location: " + lc.getRegion());
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

        for (MultipartPart part : mpp) {
            //this does NOT assume that the list comes back in sequential order
            if (part.getPartNumber() == 1) {
                Assert.assertEquals(mp1.getRawETag(), mpp.get(0).getRawETag());
            } else if (part.getPartNumber() == 2) {
                Assert.assertEquals(mp2.getRawETag(), mpp.get(1).getRawETag());
            } else if (part.getPartNumber() == 3) {
                Assert.assertEquals(mp3.getRawETag(), mpp.get(2).getRawETag());
            } else {
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
                    public MultipartPartETag call() {
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
        for (int i = 0; i < partCnt; i++) {
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
        for (InputStream uploadPartStream : uploadPartsBytesList) {
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
        for (MultipartPart part : mpp) {
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

        TreeSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>(Arrays.asList(mp1, mp2, mp3));

        client.completeMultipartUpload(new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId).withParts(parts));
    }

    @Test
    public void testPutObject() {
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
    public void testEmptyObject() {
        String key = "empty-object";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, new byte[0]);
        client.putObject(request);

        Assert.assertEquals("", client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testEmptyObjectChunked() {
        String key = "empty-object";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, "");
        client.putObject(request);

        Assert.assertEquals("", client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testEmptyObjectFile() throws IOException {
        String key = "empty-object";
        File file = File.createTempFile("empty-object-file-test", null);
        file.deleteOnExit();
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, file);
        client.putObject(request);
        Assert.assertEquals("", client.readObject(getTestBucket(), key, String.class));
    }

    @Test
    public void testPutObjectWithSpace() {
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
    public void testPutObjectWithPlus() {
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
    public void testPutObjectWithPercent() {
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
    public void testPutObjectWithChinese() {
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
    public void testPutObjectWithSmartQuote() {
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
    public void testPutObjectWithUriPunct() {
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
    public void testPutObjectWithUriReserved() {
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
    public void testPutObjectWithMd5() throws Exception {
        String key = "checksummed-object.txt";
        String content = "Hello MD5!";
        String md5B64 = Base64.encodeBase64String(DigestUtils.md5(content));

        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content.getBytes("UTF-8"));
        request.setObjectMetadata(new S3ObjectMetadata().withContentMd5(md5B64).withContentLength(content.length()));
        client.putObject(request);

        GetObjectResult<String> result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);
        Assert.assertEquals(content, result.getObject());

        // apparently S3 does not return the Content-MD5 header; it's only used during a PUT object, so use ETag
        Assert.assertEquals(md5B64, Base64.encodeBase64String(Hex.decodeHex(result.getObjectMetadata().getETag().toCharArray())));
    }

    @Test
    public void testPutObjectWithRetentionPeriod() throws Exception {
        String key = "object-in-retention";
        String content = "Hello Retention!";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content);
        request.withObjectMetadata(new S3ObjectMetadata().withRetentionPeriod(2L));
        client.putObject(request);

        Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
        try {
            client.putObject(getTestBucket(), key, "evil update!", null);
            Assert.fail("object in retention allowed update");
        } catch (S3Exception e) {
            Assert.assertEquals("ObjectUnderRetention", e.getErrorCode());
        }

        Thread.sleep(5000); // allow retention to expire
        client.putObject(getTestBucket(), key, "good update!", null);
    }

    @Test
    public void testPutObjectWithRetentionPolicy() {
        String key = "object-in-retention-policy";
        String content = "Hello Retention Policy!";
        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, content);
        request.withObjectMetadata(new S3ObjectMetadata().withRetentionPolicy("bad-policy"));
        client.putObject(request);

        // no way to verify, so if no error is returned, assume success
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
    public void testCopyObject() {
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
    public void testCopyObjectPlusSource() {
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
    public void testCopyObjectPlusDest() {
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
    public void testCopyObjectPlusBoth() {
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
    public void testCopyObjectSpaceSrc() {
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
    public void testCopyObjectSpaceDest() {
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
    public void testCopyObjectSpaceBoth() {
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
    public void testCopyObjectChineseSrc() {
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
    public void testCopyObjectChineseDest() {
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
    public void testCopyObjectChineseBoth() {
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
        Thread.sleep(2000);

        CopyObjectRequest request = new CopyObjectRequest(getTestBucket(), key, getTestBucket(), key);
        client.copyObject(request.withObjectMetadata(new S3ObjectMetadata()));
        result = client.getObject(new GetObjectRequest(getTestBucket(), key), String.class);
        Assert.assertEquals(content, result.getObject());
        Assert.assertTrue("modified date has not changed", result.getObjectMetadata().getLastModified().after(originalModified));
    }

    @Test // bug-ref: blocked by STORAGE-12050
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

    @Test // bug-ref: blocked by STORAGE-29721
    public void testUpdateMetadata() {
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
        // TODO: below assertions are blocked by STORAGE-29721,
        //  uncomment them if STORAGE-29721 is fixed
        //Assert.assertEquals(cc, objectMetadata.getCacheControl());
        //Assert.assertEquals(cd, objectMetadata.getContentDisposition());
        //Assert.assertEquals(ce, objectMetadata.getContentEncoding());
        //Assert.assertEquals(expires.getTime(), objectMetadata.getHttpExpires());
        Assert.assertEquals(userMeta, objectMetadata.getUserMetadata());
    }

    @Test
    public void testVerifyRead() {
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
        log.debug("JMC - successfully created the test object. will read object");

        Range range = new Range((long) 0, (long) (content.length() / 2));
        InputStream is = client.readObjectStream(getTestBucket(), key, range);
        log.debug("JMC - readObjectStream seemed to succeed. Will confirm the object contest");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            log.debug("JMC LINE:" + line);
        }
        log.debug("JMC - Success");
    }

    //<T> GetObjectResult<T> getObject(GetObjectRequest request, Class<T> objectType);
    @Test
    public void testGetObjectResultTemplate() {
        //creates objects named TestObject_ + zero based index
        this.createTestObjects("", 1);
        GetObjectRequest request = new GetObjectRequest(getTestBucket(), "TestObject_0");
        GetObjectResult<String> result = client.getObject(request, String.class);
        log.debug("JMC returned from client.getObject");
        log.debug("JMC getObject = " + result.getObject());
        S3ObjectMetadata meta = result.getObjectMetadata();
        log.debug("JMC meta.getContentLength(): " + meta.getContentLength());
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
    public void testDeleteObjectsRequest() {
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
        for (AbstractDeleteResult result : resultList) {
            if (result instanceof DeleteError) {
                this.inspectDeleteError((DeleteError) result);
            } else {
                this.inspectDeleteSuccess((DeleteSuccess) result);
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
    public void testDeleteObjectRequest() {
        String key = "string-test-DeleteObjectRequest";
        String content = "Hello Strings!";
        client.putObject(getTestBucket(), key, content, "text/plain");
        Assert.assertEquals(1, client.listObjects(getTestBucket()).getObjects().size());

        DeleteObjectRequest request = new DeleteObjectRequest(getTestBucket(), key);
        client.deleteObject(request);
        try {
            client.getObjectMetadata(getTestBucket(), key);
            Assert.fail("expected 404 Not Found");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
        }
    }

    @Test
    public void testGetObjectMetadata() {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(getTestBucket(), testObject);
        this.validateMetadataValues(objectMetadata);
    }

    @Test
    public void testGetObjectVersionMetadata() {
        String key = "foo", content1 = "Hello World!", content2 = "Goodbye World!";
        String mKey = "bar", mValue = "baz";
        S3ObjectMetadata meta = new S3ObjectMetadata().addUserMetadata(mKey, mValue).withContentType("text/plain");
        client.setBucketVersioning(getTestBucket(), new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));
        String v1 = client.putObject(new PutObjectRequest(getTestBucket(), key, content1).withObjectMetadata(meta)).getVersionId();
        client.deleteObject(getTestBucket(), key);
        String v2 = client.putObject(new PutObjectRequest(getTestBucket(), key, content2)).getVersionId();

        Assert.assertEquals(mValue,
                client.getObjectMetadata(new GetObjectMetadataRequest(getTestBucket(), key).withVersionId(v1)).getUserMetadata(mKey));
        Assert.assertNull(client.getObjectMetadata(new GetObjectMetadataRequest(getTestBucket(), key).withVersionId(v2)).getUserMetadata(mKey));
    }

    @Test
    public void testGetObjectMetadataNoExist() {
        String testObject = "/objectPrefix/noExist.txt";

        try {
            client.getObjectMetadata(getTestBucket(), testObject);
        } catch (S3Exception e) {
            Assert.assertEquals("Wrong HTTP status", 404, e.getHttpCode());
            Assert.assertEquals("Wrong ErrorCode", "NoSuchKey", e.getErrorCode());

            // Should not chain a SAX error
            Assert.assertNull("Should not be chained exception", e.getCause());
        }
    }

    @Test
    public void testGetObjectMetadataRequest() {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(getTestBucket(), testObject);
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(request);
        this.validateMetadataValues(objectMetadata);
    }

    protected void validateMetadataValues(S3ObjectMetadata objectMetadata) {
        Assert.assertNotNull(objectMetadata);
    }

    @Test
    public void testGetObjectAcl() {
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
    public void testGetObjectVersionAcl() {
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
    public void testSetObjectCannedAcl() {
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
        log.debug("JMC calling request.setAcl");
        request.setAcl(acl);
        client.setObjectAcl(request);

        assertAclEquals(acl, client.getObjectAcl(getTestBucket(), testObject));
    }

    @Test
    public void testSetObjectAclRequestCanned() {
        String testObject = "/objectPrefix/testObject1";
        String content = "Object Content";
        client.putObject(getTestBucket(), testObject, content, "text/plain");
        SetObjectAclRequest request = new SetObjectAclRequest(getTestBucket(), testObject);
        request.setCannedAcl(CannedAcl.BucketOwnerFullControl);
        client.setObjectAcl(request);
        //TODO - need to verify the returned acl is comparable to the canned acl
    }

    @Test
    public void testExtendObjectRetentionPeriod() throws Exception {
        String key = "object-extend-retention";
        String content = "Hello Extend Retention!";
        Long retentionPeriod = 2L;
        Long newRetentionPeriod = 5L;

        Assume.assumeTrue("ECS test bed needs to be 3.6 or later, current version: " + ecsVersion, ecsVersion != null && ecsVersion.compareTo("3.6") >= 0);

        String bucket = getTestBucket();
        PutObjectRequest request = new PutObjectRequest(bucket, key, content);
        request.withObjectMetadata(new S3ObjectMetadata().withRetentionPeriod(retentionPeriod));
        client.putObject(request);
        Assert.assertEquals(content, client.readObject(bucket, key, String.class));
        Assert.assertEquals(retentionPeriod, client.getObject(bucket, key).getObjectMetadata().getRetentionPeriod());

        client.extendRetentionPeriod(bucket, key, newRetentionPeriod);
        //Verify retention period has been extended as expected.
        Assert.assertEquals(newRetentionPeriod, client.getObject(bucket, key).getObjectMetadata().getRetentionPeriod());

        Thread.sleep(5000); // allow retention to expire
    }

    @Test
    public void testPreSignedUrl() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        s3Config.setUseV2Signer(true);
        S3Client tempClient = new S3JerseyClient(s3Config);
        URL url = tempClient.getPresignedUrl("johnsmith", "photos/puppy.jpg", new Date(1175139620000L));
        Assert.assertEquals("https://johnsmith.s3.amazonaws.com/photos/puppy.jpg" +
                        "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1175139620&Signature=NpgCjnDzrM%2BWFzoENXmpNDUsSn8%3D",
                url.toString());
    }

    @Test
    public void testPreSignedPutUrl() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        s3Config.setUseV2Signer(true);
        S3Client tempClient = new S3JerseyClient(s3Config);

        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L))
                        .withObjectMetadata(new S3ObjectMetadata().withContentType("application/x-download")
                                .withContentMd5("4gJE4saaMU4BqNR0kLY+lw==")
                                .addUserMetadata("checksumalgorithm", "crc32")
                                .addUserMetadata("filechecksum", "0x02661779")
                                .addUserMetadata("reviewedby", "joe@johnsmith.net,jane@johnsmith.net"))
        );
        Assert.assertEquals("https://static.johnsmith.net.s3.amazonaws.com/db-backup.dat.gz" +
                        "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1175139620&Signature=kPVlidlScN00QlwJMeLd9YWmpOw%3D",
                url.toString());

        s3Config = super.createS3Config();
        if(s3Config.isUseV2Signer()) {
            // test real PUT
            String key = "pre-signed-put-test", content = "This is my test object content";
            url = client.getPresignedUrl(
                    new PresignedUrlRequest(Method.PUT, getTestBucket(), key, new Date(System.currentTimeMillis() + 100000))
                            .withObjectMetadata(new S3ObjectMetadata().withContentType("application/x-download")
                                    .addUserMetadata("foo", "bar"))
            );
            Client.create().resource(url.toURI())
                    .type("application/x-download").header("x-amz-meta-foo", "bar")
                    .put(content);
            Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
            S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
            Assert.assertEquals("bar", metadata.getUserMetadata("foo"));
        }
    }

    @Test
    public void testPreSignedPutNoContentType() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        S3Client tempClient = new S3JerseyClient(s3Config);
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L)));
        Assert.assertEquals("https://static.johnsmith.net.s3.amazonaws.com/db-backup.dat.gz" +
                        "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1175139620&Signature=NnodSmujyUFr7%2Bryb8r42yY1UmM%3D",
                url.toString());

        s3Config = super.createS3Config();
        // test real PUT
        // only way is to use HttpURLConnection directly
        String key = "pre-signed-put-test-2";
        url = client.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, getTestBucket(), key, new Date(System.currentTimeMillis() + 100000))
                        .withObjectMetadata(new S3ObjectMetadata().addUserMetadata("foo", "bar")));
        // uncomment to see the next call in a proxy
//        System.setProperty("http.proxyHost", "127.0.0.1");
//        System.setProperty("http.proxyPort", "8888");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setFixedLengthStreamingMode(0);
        con.setRequestProperty("x-amz-meta-foo", "bar");
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();
        Assert.assertEquals(200, con.getResponseCode());

        Assert.assertArrayEquals(new byte[0], client.readObject(getTestBucket(), key, byte[].class));

        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals("bar", metadata.getUserMetadata("foo"));
    }

    @Test
    public void testPreSignedUrlWithChinese() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("stu").withSecretKey("/QcPo5pEvQh7EOHKs2XjzCARrt7HokZhlpdGKbHs");
        s3Config.setUseV2Signer(true);
        S3Client tempClient = new S3JerseyClient(s3Config);
        URL url = tempClient.getPresignedUrl("test-bucket", "解析依頼C1B068.txt", new Date(1500998758000L));
        Assert.assertEquals("https://test-bucket.s3.amazonaws.com/%E8%A7%A3%E6%9E%90%E4%BE%9D%E9%A0%BCC1B068.txt" +
                        "?AWSAccessKeyId=stu&Expires=1500998758&Signature=AjZv1TlZgGqlbNsLiYKFkV6gaqg%3D",
                url.toString());
    }

    @Test
    public void testPreSignedUrlWithHeaders() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true).
                withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        s3Config.setUseV2Signer(true);
        S3Client tempClient = new S3JerseyClient(s3Config);

        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(
                        Method.PUT, "johnsmith", "photos/puppy.jpg", new Date(1175139620000L))
                        .withObjectMetadata(
                                new S3ObjectMetadata().withContentType("image/jpeg")
                                        .withContentMd5("4gJE4saaMU4BqNR0kLY+lw==")));
        Assert.assertEquals("https://johnsmith.s3.amazonaws.com/photos/puppy.jpg" +
                        "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE&Expires=1175139620&Signature=FhNeBaxibG7JkmvMbJD7J11zAMU%3D",
                url.toString());
    }

    @Test
    public void testPreSignedUrlHeaderOverrides() throws Exception {
        String key = "pre-signed-header-override";
        String contentDisposition = "attachment;filename=foo.txt;";

        client.putObject(getTestBucket(), key, "", null);

        Calendar expiration = Calendar.getInstance();
        expiration.add(Calendar.HOUR, 1);
        URL url = client.getPresignedUrl(new PresignedUrlRequest(Method.GET, getTestBucket(), key, expiration.getTime())
                .headerOverride(ResponseHeaderOverride.CONTENT_DISPOSITION, contentDisposition));

        ClientResponse response = Client.create().resource(url.toURI()).get(ClientResponse.class);
        Assert.assertEquals(contentDisposition, response.getHeaders().getFirst(RestUtil.HEADER_CONTENT_DISPOSITION));
    }

    @Test
    public void testVPoolHeader() throws Exception {
        String nonDefaultVpoolID = TestConfig.getProperties().getProperty(TestProperties.NON_DEFAULT_VPOOL);
        Assume.assumeNotNull(nonDefaultVpoolID);

        String bucketName = "looney-bucket-rg";

        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName).withVPoolId(nonDefaultVpoolID);
        client.createBucket(createBucketRequest);
        // Must pause test to confirm RG has been set
        // S3 API does not allow for checking RG
        client.deleteBucket(bucketName);
    }

    /**
     * A debugging proxy (Fiddler, Charles), is required to verify that the proper header is being sent.
     * Optionally a jersey filter could be used to sniff for it
     */
    @Test
    public void testCustomHeader() {
        String customHeaderKey = "x-emc-retention-period";
        String customHeaderValue = "60";

        ListObjectsRequest request = new ListObjectsRequest(getTestBucket());

        request.addCustomHeader(customHeaderKey, customHeaderValue);

        client.listObjects(request);
    }

    @Test
    public void testStaleReadsAllowed() {
        // there's no way to test the result, so if no error is returned, assume success
        client.setBucketStaleReadAllowed(getTestBucket(), true);
    }

    @Test
    public void testMpuAbortInMiddle() throws Exception {
        String key = "mpu-abort-test";
        int partSize = 2 * 1024 * 1024;
        byte[] data = new byte[9 * 1024 * 1024];
        new Random().nextBytes(data);

        // init
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        // upload parts in background threads
        ExecutorService service = Executors.newFixedThreadPool(5);
        List<Future> futures = new ArrayList<Future>();
        int partNum = 1;
        for (int offset = 0; offset < data.length; offset += partSize) {
            int length = data.length - offset;
            if (length > partSize) length = partSize;
            final UploadPartRequest request = new UploadPartRequest(getTestBucket(), key, uploadId, partNum++,
                    Arrays.copyOfRange(data, offset, offset + length));
            futures.add(service.submit(new Runnable() {
                @Override
                public void run() {
                    client.uploadPart(request);
                }
            }));
        }

        // abort while threads are uploading
        client.abortMultipartUpload(new AbortMultipartUploadRequest(getTestBucket(), key, uploadId));

        // let threads finish
        String errorMessage = null;
        for (Future future : futures) {
            try {
                future.get();
            } catch (Throwable e) {
                while (e.getCause() != null && e.getCause() != e) e = e.getCause();
                // some versions of ECS reset the socket of ongoing uploads after an abort
                if (e instanceof SocketException && (e.getMessage().startsWith("Broken pipe")
                        || e.getMessage().startsWith("Connection reset by peer")
                        || e.getMessage().startsWith("Software caused connection abort")))
                    continue;
                if (!(e instanceof S3Exception)) throw new RuntimeException(e);
                S3Exception se = (S3Exception) e;
                if (!"NoSuchUpload".equals(se.getErrorCode()) && !"NoSuchKey".equals(se.getErrorCode()))
                    errorMessage = se.getErrorCode() + ": " + se.getMessage();
            }
        }
        Assert.assertNull(errorMessage, errorMessage);

        Assert.assertEquals(0, client.listMultipartUploads(getTestBucket()).getUploads().size());
    }

    @Test
    public void testListMarkerWithSpecialChars() {
        String marker = "foo/bar/blah%blah&blah";
        ListObjectsResult result = client.listObjects(new ListObjectsRequest(getTestBucket()).withMarker(marker)
                .withEncodingType(EncodingType.url));
        Assert.assertEquals(marker, result.getMarker());
        Assert.assertEquals(EncodingType.url, result.getEncodingType());
    }

    @Test
    public void testListPagesNoDelimiter() {
        int total = 10, page = 3;
        for (int i = 0; i < total; i++) {
            client.putObject(getTestBucket(), "key-" + i, "key-" + i, "text/plain");
        }

        Set<String> allKeys = new HashSet<String>();

        ListObjectsResult result = null;
        String nextMarker = null;
        do {
            if (result == null) result = client.listObjects(new ListObjectsRequest(getTestBucket()).withMaxKeys(page));
            else result = client.listMoreObjects(result);
            Assert.assertNotNull(result);
            if (result.isTruncated()) {
                Assert.assertEquals(page, result.getObjects().size());
                Assert.assertNotNull(result.getNextMarker());
                Assert.assertNotEquals(nextMarker, result.getNextMarker());
                nextMarker = result.getNextMarker();
            }
            for (S3Object object : result.getObjects()) {
                allKeys.add(object.getKey());
            }
        } while (result.isTruncated());

        Assert.assertEquals(total, allKeys.size());
    }

    @Test
    public void testListMarkerWithIllegalChars() {
        String marker = "foo/bar/blah\u001dblah\u0008blah";
        ListObjectsResult result = client.listObjects(new ListObjectsRequest(getTestBucket()).withMarker(marker)
                .withEncodingType(EncodingType.url));
        Assert.assertEquals(marker, result.getMarker());
    }

    @Test
    public void testPing() {
        S3Config s3Config = ((S3JerseyClient) client).getS3Config();
        String host = s3Config.getVdcs().get(0).getHosts().get(0).getName();

        PingResponse response = client.pingNode(host);
        Assert.assertNotNull(response);
        Assert.assertEquals(PingItem.Status.OFF, response.getPingItemMap().get(PingItem.MAINTENANCE_MODE).getStatus());

        response = client.pingNode(s3Config.getProtocol(), host, s3Config.getPort());
        Assert.assertNotNull(response);
        Assert.assertEquals(PingItem.Status.OFF, response.getPingItemMap().get(PingItem.MAINTENANCE_MODE).getStatus());
    }

    @Test
    public void testTimeouts() throws Exception {
        S3Config s3Config = new S3Config(Protocol.HTTP, "8.8.4.4").withIdentity("foo").withSecretKey("bar");
        s3Config.setSmartClient(((S3JerseyClient) client).getS3Config().isSmartClient());

        s3Config.setRetryLimit(0); // no retries

        // set timeouts
        int SOCKET_TIMEOUT_MILLIS = 1000; // 1 second
        int CONNECTION_TIMEOUT_MILLIS = 1000; // 1 second

        s3Config.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
        s3Config.setReadTimeout(SOCKET_TIMEOUT_MILLIS);

        final S3Client s3Client = new S3JerseyClient(s3Config);

        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            s3Client.pingNode("8.8.4.4");
            Assert.fail("response was not expected; choose an IP that is not in use");
        });

        try {
            future.get(CONNECTION_TIMEOUT_MILLIS + 100, TimeUnit.MILLISECONDS); // give an extra 100ms leeway
        } catch (TimeoutException e) {
            Assert.fail("connection did not timeout");
        } catch (ExecutionException e) {
            Assert.assertTrue(e.getCause() instanceof ClientHandlerException);
            Assert.assertTrue(e.getMessage().contains("timed out"));
        }
    }

    @Test
    public void testFaultInjection() throws Exception {
        float faultRate = 0.5f; // half of requests will fail
        S3Config s3ConfigF = new S3Config(((S3JerseyClient) client).getS3Config());
        s3ConfigF.setRetryLimit(0); // no retries (that will throw off the test)
        s3ConfigF.setFaultInjectionRate(faultRate);
        final S3Client clientF = new S3JerseyClient(s3ConfigF);

        ExecutorService executor = Executors.newFixedThreadPool(16);

        // make 100 requests
        final List<VdcHost> hosts = s3ConfigF.getVdcs().get(0).getHosts();
        int requests = 200;
        final AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (final AtomicInteger i = new AtomicInteger(); i.get() < requests; i.incrementAndGet()) {
            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        clientF.pingNode(hosts.get(i.get() % hosts.size()).getName());
                    } catch (S3Exception e) {
                        if (FaultInjectionFilter.FAULT_INJECTION_ERROR_CODE.equals(e.getErrorCode()))
                            failures.incrementAndGet();
                        else throw e;
                    }
                }
            }));
        }

        executor.shutdown();

        for (Future<?> future : futures) {
            future.get();
        }

        // roughly half should fail
        log.info("requests: " + requests + ", failures: " + failures.get());
        Assert.assertTrue(Math.abs(Math.round(faultRate * (float) requests) - failures.get()) <= requests / 10); // within 10%
    }

    @Test
    public void testCifsEcs() {
        String key = "_$folder$";

        PutObjectRequest request = new PutObjectRequest(getTestBucket(), key, new byte[0]);
        // for some stupid reason, Jersey always uses chunked transfer with "identity" content-encoding
        request.withObjectMetadata(new S3ObjectMetadata().withContentEncoding("identity"));
        client.putObject(request);
        Assert.assertNotNull(client.getObjectMetadata(getTestBucket(), key));
    }

    protected void assertAclEquals(AccessControlList acl1, AccessControlList acl2) {
        Assert.assertEquals(acl1.getOwner(), acl2.getOwner());
        Assert.assertEquals(acl1.getGrants(), acl2.getGrants());
    }

    @Test
    public void testGetPutDeleteObjectTagging() {
        // set up env
        String bucketName = getTestBucket(), key = "test-object-tagging";
        ObjectTagging t;
        client.setBucketVersioning(bucketName, new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));

        // prepare tags
        List<ObjectTag> tag = Collections.singletonList(new ObjectTag("k0","v0")); // a new [single] tag
        List<ObjectTag> tags = new ArrayList<>(); // multiple tags[10]
        IntStream.rangeClosed(1, 10).forEach(i -> tags.add(new ObjectTag("k" + i, "v" + i)));
        // more than 10 tags
        List<ObjectTag> tagsExceeded = new ArrayList<ObjectTag>(){{addAll(tags); add(new ObjectTag("k11", "v11"));}};
        // The allowed characters across services are: letters (a-z, A-Z), numbers (0-9), and spaces representable in UTF-8, and the following characters: + - = . _ : / @
        List<ObjectTag> tagMarshalXml = new ArrayList<ObjectTag>(){{add(new ObjectTag("<k", "v%"));}};
        // A tag key can be up to 128 Unicode characters in length, and tag values can be up to 256 Unicode characters in length
        String largeKey = new Random().ints(48, 122 + 1).limit(128 + 1).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        String largeValue = new Random().ints(48, 122 + 1).limit(256 + 1).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        List<ObjectTag> tagLargeKV = new ArrayList<ObjectTag>(){{add(new ObjectTag(largeKey, largeValue));}};

        // prepare PutObjectTaggingRequests
        PutObjectTaggingRequest putObjectTaggingRequestSingleTag = new PutObjectTaggingRequest(bucketName, key).withTagging(new ObjectTagging().withTagSet(tag)),
                putObjectTaggingRequestMultipleTags = new PutObjectTaggingRequest(bucketName, key).withTagging(new ObjectTagging().withTagSet(tags)),
                putObjectTaggingRequestExceededTags = new PutObjectTaggingRequest(bucketName, key).withTagging(new ObjectTagging().withTagSet(tagsExceeded)),
                putObjectTaggingRequestMarshalTag = new PutObjectTaggingRequest(bucketName, key).withTagging(new ObjectTagging().withTagSet(tagMarshalXml)),
                putObjectTaggingRequestLargeKey = new PutObjectTaggingRequest(bucketName, key).withTagging(new ObjectTagging().withTagSet(tagLargeKV));

        // get different versions of the existing object
        client.putObject(bucketName, key, "Hello VersionID0 !", "text/plain");
        String versionId = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();

        // GET the Tag of a non-existent object
        try {
            client.getObjectTagging(new GetObjectTaggingRequest(bucketName, "object-key-not-exist"));
            Assert.fail("Fail was expected. Can NOT get tags of a non-existent object");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchKey", e.getErrorCode());
        }

        // should be able to update tags for a particular version of an object, 1 ~ 10
        client.putObjectTagging(putObjectTaggingRequestSingleTag.withVersionId(versionId));
        Assert.assertEquals(1, client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key).withVersionId(versionId)).getTagSet().size());
        client.putObjectTagging(putObjectTaggingRequestMultipleTags.withVersionId(versionId));
        Assert.assertEquals(10, client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key).withVersionId(versionId)).getTagSet().size());

        // should not be able to add more than 10 tags per object
        try {
            client.putObjectTagging(putObjectTaggingRequestExceededTags.withVersionId(versionId));
            Assert.fail("Fail was expected. Can NOT add more than 10 tags per object");
        } catch (S3Exception e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("invalid content length", e.getErrorCode());
        }

        // should not be able to accept characters other than letters (a-z, A-Z), numbers (0-9), and spaces representable in UTF-8, and the following characters: + - = . _ : / @
        try {
            client.putObjectTagging(putObjectTaggingRequestMarshalTag);
            Assert.fail("Fail was expected. Can NOT accept characters other than letters (a-z, A-Z), numbers (0-9), and spaces representable in UTF-8, and the following characters: + - = . _ : / @");
        } catch (S3Exception e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("UnexpectedContent", e.getErrorCode());
        }

        // should not be able to have too large key or value.
        try {
            client.putObjectTagging(putObjectTaggingRequestLargeKey);
            Assert.fail("Fail was expected. Can NOT accept key with >128 Unicode characters in length, and value with > 256 Unicode characters in length");
        } catch (S3Exception e) {
            Assert.assertEquals(400, e.getHttpCode());
            Assert.assertEquals("UnexpectedContent", e.getErrorCode());
        }

        // GET the tag of an object where the tag was previously deleted.
        client.deleteObjectTagging(new DeleteObjectTaggingRequest(bucketName, key).withVersionId(versionId));
        t = client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key).withVersionId(versionId));
        Assert.assertNull(t.getTagSet());

    }

    @Test
    public void testGetPutDeleteObjectWithTagging() {

        // set up env
        String bucketName = getTestBucket(), key = "test-object-tagging";
        client.setBucketVersioning(bucketName, new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));
        client.putObject(new PutObjectRequest(bucketName, key, "Hello Version 1 !")
                .withObjectTagging(new ObjectTagging().withTagSet(Arrays.asList(new ObjectTag("k11", "v11"), new ObjectTag("k12", "v12")))));
        String versionId1 = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();
        client.putObject(new PutObjectRequest(bucketName, key, "Hello Version 2 !"));
        String versionId2 = client.listVersions(bucketName, key).getVersions().get(0).getVersionId();

        // Only the particular version of the object should get deleted and no other versions of object should be affected
        client.deleteObject(new DeleteObjectRequest(bucketName, key).withVersionId(versionId2));
        Assert.assertEquals(2, client.getObject(new GetObjectRequest(bucketName, key).withVersionId(versionId1), String.class).getObjectMetadata().getTaggingCount());

        // Object and associated multiple tags should get deleted
        Assert.assertEquals(2, client.getObject(new GetObjectRequest(bucketName, key).withVersionId(versionId1), String.class).getObjectMetadata().getTaggingCount());
        client.deleteObject(new DeleteObjectRequest(bucketName, key).withVersionId(versionId1));
        try {
            client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key).withVersionId(versionId1));
            Assert.fail("Fail was expected. Can NOT get tags from a deleted object");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchKey", e.getErrorCode());
        }

    }

    @Test
    public void testCopyObjectWithTagging() {

        // set up env
        String bucketName = getTestBucket(), key1 = "test-object-tagging-src", key2 = "test-object-tagging-dest1", key3 = "test-object-tagging-dest2", content = "Hello Object Tagging!", content1 = "Hello Object Tagging 1!";

        // should be able to copy the object and copied object should have the tags also
        client.putObject(new PutObjectRequest(bucketName, key1, content)
                .withObjectTagging(new ObjectTagging().withTagSet(Collections.singletonList(new ObjectTag("k11", "v11")))));
        client.copyObject(new CopyObjectRequest(bucketName, key1, bucketName, key2));
        Assert.assertEquals(1, client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key2)).getTagSet().size());

        // Versioned object should be copied and user should be able to get the same along with tags
        client.setBucketVersioning(bucketName, new VersioningConfiguration().withStatus(VersioningConfiguration.Status.Enabled));
        client.putObject(new PutObjectRequest(bucketName, key1, content1)
                .withObjectTagging(new ObjectTagging().withTagSet(Arrays.asList(new ObjectTag("k11", "v11"), new ObjectTag("k12", "v12")))));
        String versionId = client.listVersions(bucketName, key1).getVersions().get(0).getVersionId();
        client.copyObject(new CopyObjectRequest(bucketName, key1, bucketName, key3).withSourceVersionId(versionId));
        Assert.assertEquals(2, client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key3)).getTagSet().size());

    }

    @Test
    public void testMultipartUploadWithTagging() {

        // set up env
        String bucketName = getTestBucket(), key = "test-object-tagging";
        int fiveKB = 5 * 2014, i = 0;
        List<Long> sizes = new ArrayList<>();
        while (i++ < 3) {
            sizes.add(ThreadLocalRandom.current().nextLong(1, fiveKB));
        }
        RandomInputStream is1 = new RandomInputStream(sizes.get(0));
        RandomInputStream is2 = new RandomInputStream(sizes.get(1));
        RandomInputStream is3 = new RandomInputStream(sizes.get(2));

        String uploadId = client.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(bucketName, key)
                .withObjectTagging(new ObjectTagging().withTagSet(Collections.singletonList(new ObjectTag("k0","v0")))))
                .getUploadId();

        MultipartPartETag mp1 = client.uploadPart(
                new UploadPartRequest(bucketName, key, uploadId, 1, is1).withContentLength(sizes.get(0)));
        MultipartPartETag mp2 = client.uploadPart(
                new UploadPartRequest(bucketName, key, uploadId, 2, is2).withContentLength(sizes.get(1)));
        MultipartPartETag mp3 = client.uploadPart(
                new UploadPartRequest(bucketName, key, uploadId, 3, is3).withContentLength(sizes.get(2)));

        TreeSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>(Arrays.asList(mp1, mp2, mp3));
        client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, key, uploadId).withParts(parts));

        // should be able to get the tag count
        Assert.assertEquals(1, client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key)).getTagSet().size());

        // should be able to return proper error when object is deleted
        client.deleteObject(bucketName, key);
        try {
            client.getObjectTagging(new GetObjectTaggingRequest(bucketName, key));
            Assert.fail("Fail was expected. Can NOT get tags from a deleted object");
        } catch (S3Exception e) {
            Assert.assertEquals(404, e.getHttpCode());
            Assert.assertEquals("NoSuchKey", e.getErrorCode());
        }
    }

}
