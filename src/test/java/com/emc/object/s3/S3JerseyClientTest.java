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
        List<Bucket> bucketList = result.getBuckets();
        l4j.debug("There are " + Integer.toString(bucketList.size()) + " existing buckets");
        for (Bucket b: bucketList) {
            l4j.debug("JMC bucket: " + b.getName());
        }
    }
    
    @Test
    public void testBucketExists() throws Exception {
        Assert.assertTrue("Bucket " + getTestBucket() + " should exist but does NOT", client.bucketExists(getTestBucket()));
        l4j.debug("JMC testBucketExists succeeded!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    @Test
    public void testCreateBucketRequest() throws Exception {
        String bucketName = getTestBucket() + "-x";
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        client.createBucket(request);
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
        createTestObjects(getTestBucket(), "prefix/", 5);
        l4j.debug("Objects in bucket " + getTestBucket() + " have been created");
        try {
            client.deleteBucket(getTestBucket());
            Assert.fail("Test succeeds. Fail was expected. Can NOT delete bucket with existing objects");
        } catch (S3Exception e) {
            Assert.assertEquals("wrong error code for delete non-empty bucket", "BucketNotEmpty", e.getErrorCode());
        }
    }

    protected void assertSameAcl(AccessControlList acl1, AccessControlList acl2) {
        Assert.assertEquals(acl1.getOwner().getId(), acl2.getOwner().getId());

        Set<Grant> gs1 = acl1.getGrants();
        Set<Grant> gs2 = acl2.getGrants();

        Assert.assertEquals(gs1.size(), gs2.size());
        Iterator<Grant> grantI = acl2.getGrants().iterator();
        for (Grant g1 : acl1.getGrants()) {
            Grant g2 = grantI.next();
            Assert.assertEquals(g1.getGrantee(), g2.getGrantee());
            Assert.assertEquals(g1.getPermission(), g2.getPermission());
        }
    }

    @Test
    public void testSetBucketAcl() throws Exception {
        String identity = createS3Config().getIdentity();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setBucketAcl(getTestBucket(), acl);

        this.assertSameAcl(acl, client.getBucketAcl(getTestBucket()));
    }

    @Ignore // TODO: blocked by STORAGE-7422
    @Test
    public void testSetBucketAclCanned() {
        client.setBucketAcl(getTestBucket(), CannedAcl.BucketOwnerFullControl);
    }

    //TODO
    //void setBucketAcl(SetBucketAclRequest request);
    
    //tested by testSetBucketAcl
    //AccessControlList getBucketAcl(String bucketName);

    //tested by testGetBucketCors
    //void setBucketCors(String bucketName, CorsConfiguration corsConfiguration);

    
    @Test
    public void testGetBucketCors() throws Exception {
        //CorsConfiguration getBucketCors(String bucketName);

        ArrayList<CorsRule> crArr = new ArrayList<CorsRule>();
        List<CorsRule> crArrVerify = new ArrayList<CorsRule>();

        CorsRule cr0 = new CorsRule();
        cr0.setId("corsRuleTestId0");
        cr0.withAllowedOrigins("10.10.10.10");
        cr0.withAllowedMethods(CorsMethod.GET);
        crArr.add(cr0);
        crArrVerify.add(cr0);

        CorsRule cr1 = new CorsRule();
        cr1.setId("corsRuleTestId1");
        cr1.withAllowedOrigins("10.10.10.10");
        cr1.withAllowedMethods(CorsMethod.GET);
        crArr.add(cr1);
        crArrVerify.add(cr1);

        CorsRule cr2 = new CorsRule();
        cr2.setId("corsRuleTestId2");
        cr2.withAllowedOrigins("10.10.10.10");
        cr2.withAllowedMethods(CorsMethod.GET);
        crArr.add(cr2);
        crArrVerify.add(cr2);

        CorsConfiguration cc = new CorsConfiguration();
        cc.setCorsRules(crArr);

        client.setBucketCors(getTestBucket(), cc);

        CorsConfiguration ccVerify = client.getBucketCors(getTestBucket());
        Assert.assertNotNull("CorsConfiguration should NOT be null but is", ccVerify);
        crArrVerify = ccVerify.getCorsRules();
        Assert.assertNotNull("CorsRule list should NOT be null but is", crArrVerify);
        Assert.assertEquals("There are NOT the same number of CorsRule items", crArr.size(), crArrVerify.size());
 
        //JMC the rules might not come back in the same order! need to change
        //maybe could brute force or look into sort or rules (based upon id maybe)
        int testResults = 0x0;
        for (CorsRule aCrArrVerify : crArrVerify) {
            if (cr0.getId().equals(aCrArrVerify.getId())) {
                //Assert.assertEquals(crArr.get(i).getId(), crArrVerify.get(i).getId());
                testResults = testResults | 0x001;
            }
            if (cr1.getId().equals(aCrArrVerify.getId())) {
                //Assert.assertEquals(crArr.get(i).getId(), crArrVerify.get(i).getId());
                testResults = testResults | 0x010;
            }
            if (cr1.getId().equals(aCrArrVerify.getId())) {
                //Assert.assertEquals(crArr.get(i).getId(), crArrVerify.get(i).getId());
                testResults = testResults | 0x100;
            }
        }
        Assert.assertEquals("Incorrect CorRules returned", 0x111, testResults);
    }

    
    @Test
    public void testDeleteBucketCors() throws Exception {
        ArrayList<CorsRule> crArr = new ArrayList<CorsRule>();

        CorsRule cr = new CorsRule();
        cr.setId("corsRuleTestId");
        cr.withAllowedMethods(CorsMethod.GET).withAllowedOrigins("10.10.10.10");
        crArr.add(cr);

        CorsConfiguration cc = new CorsConfiguration();
        cc.setCorsRules(crArr);
        client.setBucketCors(getTestBucket(), cc);

        CorsConfiguration ccVerify = client.getBucketCors(getTestBucket());
        Assert.assertNotNull("deleteBucketCors NOT tested ccVerify prereq is null", ccVerify);

        client.deleteBucketCors(getTestBucket());//PRIMARY TEST CALL
        try {
            client.getBucketCors(getTestBucket());
            Assert.fail("getting non-existing cors config should throw exception");
        } catch (S3Exception e) {
            Assert.assertEquals("Wrong error code when getting non-existing cors config", "NoSuchCORSConfiguration", e.getErrorCode());
        }

        //make sure the bucket still exists
        Assert.assertTrue("deleteBucketCors succeeded, but bucket does not exist", client.bucketExists(getTestBucket()));
    }
    
    //see testDeleteBucketLifecycle
    //void setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfiguration);

    //see testDeleteBucketLifecycle
    //LifecycleConfiguration getBucketLifecycle(String bucketName);

    
    //TODO
    @Test
    public void testDeleteBucketLifecycle() throws Exception {
        String bn = getTestBucket();
        LifecycleRule lcr = new LifecycleRule();
        ArrayList<LifecycleRule> lcrList = new ArrayList<LifecycleRule>();
        lcrList.add(lcr);
        LifecycleConfiguration lc = new LifecycleConfiguration();
        lc.setRules(lcrList);

        //String bucketName = createBucketAndName();
        client.setBucketLifecycle(bn, lc);
        Assert.assertNotNull(client.getBucketLifecycle(bn));
        client.deleteBucketLifecycle(bn); //PRIMARY TEST CALL
        try {
            client.getBucketLifecycle(bn);
            Assert.fail("getting non-existing bucket lifecycle should throw exception");
        } catch (S3Exception e) {
            Assert.assertEquals("wrong error code for getting non-existing bucket lifecycle", "NoSuchBucketPolicy", e.getErrorCode());
        }

        //make sure the bucket still exists
        Assert.assertTrue("deleteBucketLifecycle succeeded, but bucket does not exist", client.bucketExists(bn));
        //cleanUpBucket(bn);
    }
    
    @Test 
    public void testListObjectsLor() throws Exception {
        ListObjectsRequest request = new ListObjectsRequest(getTestBucket());

        ListObjectsResult result = client.listObjects(request);
        Assert.assertNotNull("ListObjectsResult was null, but should NOT have been", result);
        List<S3Object> resultObjects = result.getObjects();
        Assert.assertNotNull("List<S3Object> was null, but should NOT have been", resultObjects);

        //TODO after createObject works need to test that the resultObjects list is correct
    }
    
    @Test
    public void testListObjectsBucketName() throws Exception {
        ListObjectsResult result = client.listObjects(getTestBucket());

        Assert.assertNotNull("ListObjectsResult was null, but should NOT have been", result);
        List<S3Object> resultObjects = result.getObjects();
        Assert.assertNotNull("List<S3Object> was null, but should NOT have been", resultObjects);

        //TODO after createObject works need to test that the resultObjects list is correct
    }
    
    @Test
    public void testListObjectsBucketNamePrefix() throws Exception {
        l4j.debug("JMC Entered testListObjectsBucketNamePrefix");
        String myPrefix = "testPrefix";
        int numObjects = 10;
        this.createTestObjects("testPrefix/", numObjects);
        l4j.debug("JMC created all test objects. Now they will be listed");
        ListObjectsResult result = client.listObjects(getTestBucket(), myPrefix);
        Assert.assertNotNull("ListObjectsResult was null and should NOT be", result);
        Assert.assertEquals("The correct number of objects were NOT returned", numObjects, result.getObjects().size());
        l4j.debug("JMC testListObjectsBucketNamePrefix succeeded. Going to print object names!!!!!");
        l4j.debug(Integer.toString(result.getObjects().size()) + " were returned");
        //List<S3Object> s3Objects = result.getObjects();
        int tempInt = 0;
        for (S3Object s3Object : result.getObjects()) {
            l4j.debug("s3Object[" + Integer.toString(tempInt) + "]: " + s3Object.getKey());
            tempInt++;
        }
    }


    /*
     * just a basic listing of objects with paged results (ie no prefix matching)
     */
    @Test
    public void testListObjectsPaging1() throws Exception {
        l4j.debug("JMC Entered testListObjectsBucketNamePrefix");
        String myPrefix = "testPrefix";
        int numObjects = 10;
        int pageSize = 5;
        int loopCnt = (numObjects+pageSize-1)/pageSize;

        this.createTestObjects(myPrefix, numObjects);
        l4j.debug("JMC created all test objects. Now they will be listed");

        List<S3Object> s3ObjectList = new ArrayList<S3Object>();
        ListObjectsResult result;
        ListObjectsRequest request = new ListObjectsRequest(getTestBucket());
        request.setMaxKeys(pageSize);
        for (int i=0; i<loopCnt;i++) {
            result = client.listObjects(request);
            s3ObjectList.addAll(result.getObjects());
            request.setMarker(result.getNextMarker());
        }

        Assert.assertEquals("The correct number of objects were NOT returned", numObjects, s3ObjectList.size());
        l4j.debug("JMC testListObjectsBucketNamePrefix succeeded. Going to print object names!!!!!");
        l4j.debug(Integer.toString(s3ObjectList.size()) + " were returned");
        //List<S3Object> s3Objects = result.getObjects();
        int tempInt = 0;
        for (S3Object s3Object : s3ObjectList) {
            l4j.debug("s3Object[" + Integer.toString(tempInt) + "]: " + s3Object.getKey());
            tempInt++;
        }
    }

    /*
     * listing of objects with paged results only on those objects with matching prefix
     */
    @Test
    public void testListObjectsPagingWithPrefix() throws Exception {
        l4j.debug("JMC Entered testListObjectsBucketNamePrefix");
        String myPrefixA = "testPrefixA";
        String myPrefixB = "testPrefixB";
        int numObjects = 10;
        int pageSize = 5;
        int loopCnt = (numObjects+pageSize-1)/pageSize;

        this.createTestObjects(myPrefixA, numObjects);
        this.createTestObjects(myPrefixB, numObjects);
        l4j.debug("JMC created all test objects. Now they will be listed");

        List<S3Object> s3ObjectList = new ArrayList<S3Object>();
        ListObjectsResult result;
        ListObjectsRequest request = new ListObjectsRequest(getTestBucket());
        request.setPrefix(myPrefixA);
        request.setMaxKeys(pageSize);
        for (int i=0; i<loopCnt;i++) {
            result = client.listObjects(request);
            s3ObjectList.addAll(result.getObjects());
            request.setMarker(result.getNextMarker());
        }

        Assert.assertEquals("The correct number of objects were NOT returned", numObjects, s3ObjectList.size());
        l4j.debug("JMC testListObjectsBucketNamePrefix succeeded. Going to print object names!!!!!");
        l4j.debug(Integer.toString(s3ObjectList.size()) + " were returned");
        //List<S3Object> s3Objects = result.getObjects();
        int tempInt = 0;
        for (S3Object s3Object : s3ObjectList) {
            l4j.debug("s3Object[" + Integer.toString(tempInt) + "]: " + s3Object.getKey());
            tempInt++;
        }
    }

    @Test
    public void testCreateBucket() throws Exception {
        String bn = getTestBucket();
        //String bucketName = "TestBucket_" + UUID.randomUUID();
        //client.createBucket(bucketName);
        Assert.assertNotNull("testCreateBucket failed for bucket: " + bn,  client.bucketExists(bn));
    }
    
    //tested in testGetBucketCors
    //void setBucketCors(String bucketName, CorsConfiguration corsConfiguration);
    
    
    //ListVersionsResult listVersions(String bucketName, String prefix);
    @Test
    public void testListVersions() throws Exception {
        l4j.debug("JMC Entered testListVersions");
        ListVersionsResult lvr = client.listVersions(getTestBucket(), getTestBucketPrefix());
        Assert.assertNotNull(lvr.getVersions());
        //List<AbstractVersion> vList = lvr.getVersions();
        //l4j.debug("JMC vList.size() = " + vList.size());
        Assert.assertNotNull(lvr.getBucketName());
        //Assert.assertNotNull(lvr.getDelimiter());
        //Assert.assertNotNull(lvr.getKeyMarker());
        //Assert.assertNotNull(lvr.getNextKeyMarker());
        Assert.assertNotNull(lvr.getPrefix());
        //Assert.assertNotNull(lvr.getVersionIdMarker());
        //Assert.assertNotNull(lvr.getCommonPrefixes());
        //Assert.assertNotNull(lvr.getMaxKeys());
        //Assert.assertNotNull(lvr.getTruncated());
    }

    @Test
    public void testListVersionsReq() throws Exception {
        ListVersionsRequest request = new ListVersionsRequest(getTestBucket());
        request.setPrefix(getTestBucketPrefix());
        ListVersionsResult lvr = client.listVersions(request);
        Assert.assertNotNull(lvr.getBucketName());
        //Assert.assertNotNull(lvr.getDelimiter());
        //Assert.assertNotNull(lvr.getKeyMarker());
        //Assert.assertNotNull(lvr.getNextKeyMarker());
        Assert.assertNotNull(lvr.getPrefix());
        //Assert.assertNotNull(lvr.getVersionIdMarker());
        //Assert.assertNotNull(lvr.getCommonPrefixes());
        //Assert.assertNotNull(lvr.getMaxKeys());
        //Assert.assertNotNull(lvr.getTruncated());
        Assert.assertNotNull(lvr.getVersions());
    }
    
    protected void createTestObjects(String prefixWithDelim, int numObjects) throws Exception {
        this.createTestObjects(getTestBucket(), prefixWithDelim, numObjects);
    }

    protected void createTestObjects(String bucket, String prefixWithDelim, int numObjects) throws Exception {
        String objectName;

        byte[] content1 = new byte[5 * 1024];
        new Random().nextBytes(content1);

        for(int i=0; i<numObjects; i++) {
            objectName = "TestObject_" + i;
            client.putObject(bucket, prefixWithDelim + objectName, content1, "text/plain");
        }
    }

    //TODO need to actually make these multi part uploads
    //returns ArrayList of [ [uploadId,key],[uploadId,key]... ]
    protected List<List<String>> createMultipartTestObjects(String prefix, int numObjects) throws Exception {
        List<List<String>> upIds = new ArrayList<List<String>>();
        ArrayList<String> tmp;
        String objectName;

        for(int i=0; i<numObjects; i++) {
            objectName = "TestObject_" + UUID.randomUUID();
            tmp = new ArrayList<String>();
            tmp.add(client.initiateMultipartUpload(getTestBucket(), prefix + objectName));
            tmp.add(prefix + objectName);
            upIds.add(tmp);
        }
        return upIds;
    }
    
    @Test
    public void testCreateReadObject() throws Exception {
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

    //TODO
    //@Test
    public void testInitiateListAbortMultipartUploads() throws Exception {
        int numObjects = 2;
        //String prefix = "multiPrefix/";
        //List<List<String>> upIds = this.createMultipartTestObjects(prefix, numObjects);

        ListMultipartUploadsResult result = client.listMultipartUploads(getTestBucket());
        Assert.assertNotNull(result);
        List<Upload> lu = result.getUploads();
        Assert.assertEquals(numObjects, lu.size());
    }

    //TODO
    // ListMultipartUploadsResult listMultipartUploads(ListMultipartUploadsRequest request);


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
            } while (listPartsResult.getTruncated());

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

    //TODO
    //@Test
    public void testUpdateObject() throws Exception {
        //create the initial object
        client.putObject(getTestBucket(), "testObject1", "Hello Create!", "text/plain");

        //TODO figure out this Range class thing
        //client.updateObject(getTestBucket(), "testObect1", range, content);
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
            Assert.assertNotNull(((Version) version).geteTag());
            Assert.assertNotNull(((Version) version).getStorageClass());
            if (version.getLatest()) {
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
            if (version.getLatest()) thirdVersionId = version.getVersionId();
        }

        // delete object (creates a delete marker)
        client.deleteObject(getTestBucket(), key);

        versions = client.listVersions(getTestBucket(), null).getVersions();
        Assert.assertEquals(4, versions.size());
        String fourthVersionId = null;
        for (AbstractVersion version : versions) {
            if (version.getLatest()) fourthVersionId = version.getVersionId();
        }

        // delete explicit versions, which should revert back to prior version
        client.deleteVersion(getTestBucket(), key, fourthVersionId);

        versions = client.listVersions(getTestBucket(), null).getVersions();
        Assert.assertEquals(3, versions.size());
        for (AbstractVersion version : versions) {
            if (version.getLatest()) Assert.assertEquals(thirdVersionId, version.getVersionId());
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
    public void testSetObjectAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        client.putObject(getTestBucket(), testObject, "Hello ACLs!", "text/plain");

        String identity = createS3Config().getIdentity();
        CanonicalUser owner = new CanonicalUser(identity, identity);
        AccessControlList acl = new AccessControlList();
        acl.setOwner(owner);
        acl.addGrants(new Grant(owner, Permission.FULL_CONTROL));

        client.setObjectAcl(getTestBucket(), testObject, acl);
        assertSameAcl(acl, client.getBucketAcl(getTestBucket()));
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

        assertSameAcl(acl, client.getObjectAcl(getTestBucket(), testObject));
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

    //TODO: AccessControlList getObjectAcl(String bucketName, String key);
}
