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

import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import com.emc.object.util.InputStreamSegment;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class S3JerseyClientTest extends AbstractS3ClientTest {
    private static final Logger l4j = Logger.getLogger(S3JerseyClientTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            l4j.debug("Starting test:>>>>>>>>>>>>>>> " + description.getMethodName());
        }
    };

    @Rule
    public ExpectedException thrown= ExpectedException.none();
   
    @Override
    protected String getTestBucketPrefix() {
        return "s3-client-test";
    }

    @Override
    public void initClient() throws Exception {
        client = new S3JerseyClient(createS3Config());
    }

    @Test
    public void emptyTest() throws Exception {
        l4j.debug("JMC Entered empty test to ensure Before/After processes");
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

        l4j.debug("JMC testListBuckets succeeded for user: " + result.getOwner().getId() + " !!!!!!!!!!!!!!!!!!!");
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

    //this is tested by default in the @Before method
    //void createBucket(String bucketName);
    @Test
    public void testCreateBucketRequest() throws Exception {
        String bucketName = getTestBucket() + "-x";
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        client.createBucket(request);
        this.cleanUpBucket(bucketName);
    }

    @Test
    public void testDeleteBucket() throws Exception {
        String bucketName = getTestBucket() + "-x";
        Assert.assertFalse("bucket should not exist " + bucketName, client.bucketExists(bucketName));

        client.createBucket(bucketName);
        Assert.assertTrue("failed to create bucket " + bucketName, client.bucketExists(bucketName));

        client.deleteBucket(bucketName);
        client.bucketExists(bucketName); // workaround for STORAGE-3299
        Assert.assertFalse("failed to delete bucket " + bucketName, client.bucketExists(bucketName));

        //JMC need to note that the @After cleanup will fail
        l4j.debug("JMC - deleteBucket seemed to work");
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

    protected AccessControlList createAcl() {
        CanonicalUser cu1 = new CanonicalUser("user1", "userDisplayName1");
        Permission perm = Permission.FULL_CONTROL;
        Grant grant = new Grant(cu1, perm);

/*
        CanonicalUser cu2 = new CanonicalUser("user1","userDisplayName2");
        Permission perm2 = Permission.READ;
        Grant grant2 = new Grant(cu2, perm2);
        */

        Set<Grant> grantSet = new HashSet<Grant>();
        grantSet.add(grant);
        /*
        grantSet.add(grant2);
        */

        AccessControlList acl = new AccessControlList();
        AccessControlList origAcl = client.getBucketAcl(getTestBucket());
        CanonicalUser cu = origAcl.getOwner();
        acl.setOwner(cu);
        acl.setGrants(grantSet);
        return acl;
    }
    
    protected void testAclsEqual(AccessControlList acl1, AccessControlList acl2) {
        CanonicalUser owner1 = acl1.getOwner();
        CanonicalUser owner2 = acl2.getOwner();
        Assert.assertEquals(owner1.getId(), owner2.getId());
        Assert.assertEquals(owner1.getDisplayName(), owner2.getDisplayName());

        /*
        Grant[] grants1 = (Grant[])acl1.getGrants().toArray();
        Grant[] grants2 = (Grant[])acl2.getGrants().toArray();
        Assert.assertEquals(grants1.length, grants2.length);
        */
        Set<Grant> gs1 = acl1.getGrants();
        Set<Grant> gs2 = acl2.getGrants();

        //should only be 1 grant per acl for current testing at this point. There
        //used to be 2 but I removed the second one for now (see this.createAcl())
        Assert.assertEquals(gs1.size(), gs2.size());
        Grant g1 = new Grant();
        Grant g2 = new Grant();
        for (Grant g: gs1) {
            g1 = g;
            l4j.debug("JMC retrieved g1");
        }
        for (Grant g: gs2) {
            g2 = g;
            l4j.debug("JMC retrieved g2");
        }
        //Grant implements comparable
        Assert.assertEquals(g1, g2);
    }

    @Test
    public void testSetBucketAcl() throws Exception {
        AccessControlList acl = this.createAcl();
        client.setBucketAcl(getTestBucket(), acl);
        AccessControlList aclReturned = client.getBucketAcl(getTestBucket());
        this.testAclsEqual(acl, aclReturned);
    }

    //TODO
    public void testSetBucketAclCanned() {
        //void setBucketAcl(String bucketName, CannedAcl cannedAcl);
        client.setBucketAcl(getTestBucket(), CannedAcl.BucketOwnerFullControl);
        AccessControlList acl = client.getBucketAcl(getTestBucket());
        SetBucketAclRequest request;
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
        l4j.debug("JMC Entered createTestObjects. Creating " + Integer.toString(numObjects));
        String objectName;
        File testFile = new File(System.getProperty("user.home") + File.separator + "test.properties");

        int fiveKB = 5 * 1024;
        byte[] content1 = new byte[5 * 1024];
        new Random().nextBytes(content1);

        if (!testFile.exists()) {
            throw new FileNotFoundException("test.properties");
        }
        
        for(int i=0; i<numObjects; i++) {
            //objectName = "TestObject_" + UUID.randomUUID();
            objectName = "TestObject_" + Integer.toString(i);
            l4j.debug("JMC about to create " + objectName);
            //client.putObject(bucket, prefixWithDelim + objectName, testFile, "text/plain");
            client.putObject(bucket, prefixWithDelim + objectName, content1, "text/plain");
            l4j.debug("JMC client.createObject " + objectName + " seemed to work");
        }
        l4j.debug("JMC Done creating test objects");
    }

    //TODO need to actually make these multi part uploads
    //returns ArrayList of [ [uploadId,key],[uploadId,key]... ]
    protected List<List<String>> createMultipartTestObjects(String prefix, int numObjects) throws Exception {
        List<List<String>> upIds = new ArrayList<List<String>>();
        ArrayList<String> tmp;
        String objectName;
        File testFile = new File(System.getProperty("user.home") + File.separator +"test.properties");
        if(!testFile.exists()) {
            throw new FileNotFoundException("test.properties");
        }
        
        for(int i=0; i<numObjects; i++) {
            objectName = "TestObject_" + UUID.randomUUID();
            tmp = new ArrayList<String>();
            System.out.println("JMC about to call client.initiateMultipartUpload");
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
    public void testCreateObjectWithRange() throws Exception {
        //void putObject(String bucketName, String key, Range range, Object content)
        l4j.debug("JMC Entered testCreateObjectWithRange");
        String fileName = System.getProperty("user.home") + File.separator + "test.properties";
        File testFile = new File(fileName);
        if (!testFile.exists()) {
            throw new FileNotFoundException("test.properties");
        }
        long length = testFile.length();
        Range range = new Range((long)0, length/2);
        client.putObject(getTestBucket(), "/objectPrefix/testObject1", range, testFile);
        l4j.debug("Put the first half of the object");
        range = new Range(length / 2, length / 2);
        client.putObject(getTestBucket(), "/objectPrefix/testObject2", range, testFile);
        l4j.debug("Put both halves of the file");
    }

    @Test
    public void testCreateObjectWithRequest() throws Exception {
        String fileName = System.getProperty("user.home") + File.separator + "test.properties";
        //PutObjectResult putObject(PutObjectRequest request);
        //PutObjectRequest(String bucketName, String key, T object) {
        PutObjectRequest<String> request = new PutObjectRequest<String>(getTestBucket(), "/objectPrefix/testObject1", fileName);
        PutObjectResult result = client.putObject(request);
        Assert.assertNotNull(result);
    }

    @Test
    public void testCreateObjectChunkedWithRequest() throws Exception {
        //request.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        //String fileName = System.getProperty("user.home") + File.separator + "test.properties";
        int size = 50000;
        byte[] data =  new byte[size];
        new Random().nextBytes(data);
        String dataStr = new String(data);
        PutObjectRequest<String> request = new PutObjectRequest<String>(getTestBucket(), "/objectPrefix/testObject1", dataStr);
        //request.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

        //request.property(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, -1);
        //System.out.println("JMC PROPERTY_CHUNKED_ENCODING_SIZE to -1");

        request.property(ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING, Boolean.FALSE);
        System.out.println("JMC PROPERTY_ENABLE_BUFFERING to false");
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
        client.putObject(new PutObjectRequest<String>(getTestBucket(), key, content).withObjectMetadata(objectMetadata));
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
    public void testInitiateListAbortMultipartUploads() throws Exception {
        int numObjects = 2;
        String prefix = "multiPrefix/";
        System.out.println("JMC about to call this.createMultipartTestObjects ");
        List<List<String>> upIds = this.createMultipartTestObjects(prefix, numObjects);

        System.out.println("JMC about to call client.listMultipartUploads");
        ListMultipartUploadsResult result = client.listMultipartUploads(getTestBucket());
        Assert.assertNotNull(result);
        List<Upload> lu = result.getUploads();
        Assert.assertEquals(numObjects, lu.size());

        System.out.println("JMC testInitiateListAbortMultipartUploads ListMultipartUploadsResult.length = " + lu.size());
        //TODO - Upload members are private with no getter/setters
        /*
        for (Upload u: lu) {
            System.out.println("JMC - ListMultipartUploadsResult uploadID: ");
        }
        */
        System.out.println("JMC createMultipartTestObjects returned = " + upIds.size());
        //ArrayList of [ [uploadId,key],[uploadId,key]... ]
        for (List<String> tmp: upIds) {
            System.out.println("JMC createMultipartTestObjects uploadId: " + tmp.get(0) + "\tkey: " + tmp.get(1));
            //AbortMultipartUploadRequest(String bucketName, String key, String uploadId)
            System.out.println("JMC - now aborting initialized upload");
            AbortMultipartUploadRequest abortReq = new AbortMultipartUploadRequest(getTestBucket(), tmp.get(1), tmp.get(0));
        }
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

        System.out.println("JMC - calling client.initiateMultipartUpload");
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);
        System.out.println("JMC - calling client.UploadPartRequest 1");
        MultipartPartETag mp1 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1,
                new InputStreamSegment(is1, 0, fiveKB)));

        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        System.out.println("JMC - calling client.completeMultipartUpload");
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        CompleteMultipartUploadResult completionResult = client.completeMultipartUpload(completionRequest);
        System.out.println("JMC - returned from client.completeMultipartUpload");
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

        System.out.println("JMC - calling client.initiateMultipartUpload");
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);
        System.out.println("JMC - calling client.UploadPartRequest 1");
        MultipartPartETag mp1 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1,
                new InputStreamSegment(is1, 0, fiveKB)));
        System.out.println("JMC - calling client.UploadPartRequest 2");
        MultipartPartETag mp2 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 2,
                new InputStreamSegment(is2, 0, fiveKB)));
        System.out.println("JMC - calling client.UploadPartRequest 3");
        MultipartPartETag mp3 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 3,
                new InputStreamSegment(is3, 0, fiveKB)));

        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        parts.add(mp2);
        parts.add(mp3);


        ListPartsResult lpr = client.listParts(getTestBucket(), key, uploadId);
        l4j.debug("ListPartsResult bucketName: " + lpr.getBucketName());
        l4j.debug("ListPartsResult key: " + lpr.getKey());
        l4j.debug("ListPartsResult getNextPartNumberMarker: " + lpr.getNextPartNumberMarker());
        l4j.debug("ListPartsResult uploadId: " + lpr.getUploadId());
        l4j.debug("ListPartsResult getPartNumberMarker: " + lpr.getPartNumberMarker());
        l4j.debug("------------------Start list of Multiparts----------------------");
        List<MultipartPart> mpp = lpr.getParts();
        Assert.assertEquals(3, mpp.size());

        for (MultipartPart part: mpp) {
            l4j.debug("\t----------------------part#" + part.getPartNumber() + " information");
            l4j.debug("\tpart.getSize() = " + part.getSize());
            l4j.debug("\tpart.getETag = " + part.getETag() );
            l4j.debug("\tpart.getPartNumber = " + part.getPartNumber() );
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
        l4j.debug("------------------End list of Multiparts----------------------");

        System.out.println("JMC - calling client.completeMultipartUpload");
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        CompleteMultipartUploadResult completionResult = client.completeMultipartUpload(completionRequest);
        System.out.println("JMC - returned from client.completeMultipartUpload");
    }


    @Test
    public void testMultiThreadMultipartUploadListPartsPagination() throws Exception {
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
        l4j.debug("JMC - calling client.initiateMultipartUpload");
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        List<Future<?>> futures = new ArrayList<Future<?>>();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        final AtomicInteger successCount = new AtomicInteger();
        int uploadPartNumber = 1;
        for(InputStream uploadPartStream: uploadPartsBytesList) {
            final UploadPartRequest request = new UploadPartRequest(getTestBucket(), key, uploadId, uploadPartNumber,
                    new InputStreamSegment(uploadPartStream, 0, fiveKB));

            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    l4j.debug(Thread.currentThread().getName() + " thread uploading part number: " + request.getPartNumber());
                    client.uploadPart(request);
                    successCount.incrementAndGet();
                }
            }));
            uploadPartNumber++;
        }
        l4j.debug("JMC submitted all parts into the executor service. About to shutdown and wait");
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        l4j.debug("JMC all threads terminated. The wait is complete");
        Assert.assertEquals("at least one thread failed", futures.size(), successCount.intValue());

        int maxParts = 2;
        int loopCnt = (partCnt+maxParts-1)/maxParts;

        l4j.debug("JMC will need to make " + Integer.toString(loopCnt) + " listParts calls");
        ListPartsRequest listPartsRequest = new ListPartsRequest(getTestBucket(), key, uploadId);
        listPartsRequest.setMaxParts(maxParts);
        ListPartsResult lpr;
        String marker = "";
        List<MultipartPart> mpp = new ArrayList<MultipartPart>();
        for (int i=0; i<loopCnt; i++) {
            lpr = client.listParts(listPartsRequest);
            mpp.addAll(lpr.getParts());
            marker = lpr.getNextPartNumberMarker();
            l4j.debug("JMC setting the marker for the next listParts request marker: " + marker);
            listPartsRequest.setMarker(marker);
        }

        l4j.debug("JMC - calling client.completeMultipartUpload");
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();

        MultipartPartETag eTag;
        for (MultipartPart part: mpp) {
            l4j.debug("JMC adding part# " + part.getPartNumber()   + " with etag: " + part.getETag() );
            eTag = new MultipartPartETag(part.getPartNumber(), part.getETag());
            parts.add(eTag);
        }
        completionRequest.setParts(parts);
        l4j.debug("JMC - set the parts for the completion request");
        CompleteMultipartUploadResult completionResult = client.completeMultipartUpload(completionRequest);
        l4j.debug("JMC - returned from client.completeMultipartUpload");
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
        l4j.debug("JMC - calling client.initiateMultipartUpload");
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);

        List<Future<?>> futures = new ArrayList<Future<?>>();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        final AtomicInteger successCount = new AtomicInteger();
        int uploadPartNumber = 1;
        for(InputStream uploadPartStream: uploadPartsBytesList) {
            final UploadPartRequest request = new UploadPartRequest(getTestBucket(), key, uploadId, uploadPartNumber,
                    new InputStreamSegment(uploadPartStream, 0, fiveKB));

            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    l4j.debug(Thread.currentThread().getName() + " thread uploading part number: " + request.getPartNumber());
                    client.uploadPart(request);
                    successCount.incrementAndGet();
                }
            }));
            uploadPartNumber++;
        }
        l4j.debug("JMC submitted all parts into the executor service. About to shutdown and wait");
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        l4j.debug("JMC all threads terminated. The wait is complete");
        Assert.assertEquals("at least one thread failed", futures.size(), successCount.intValue());

        ListPartsResult lpr = client.listParts(getTestBucket(), key, uploadId);
        List<MultipartPart> mpp = lpr.getParts();
        Assert.assertEquals("at least one part failed according to listParts", successCount.intValue(), mpp.size());


        l4j.debug("JMC - calling client.completeMultipartUpload");
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        MultipartPartETag eTag;
        for (MultipartPart part: mpp) {
            l4j.debug("JMC adding part# " + part.getPartNumber()   + " with etag: " + part.getETag() );
            eTag = new MultipartPartETag(part.getPartNumber(), part.getETag());
            parts.add(eTag);
        }
        completionRequest.setParts(parts);
        l4j.debug("JMC - set the parts for the completion request");
        CompleteMultipartUploadResult completionResult = client.completeMultipartUpload(completionRequest);
        l4j.debug("JMC - returned from client.completeMultipartUpload");
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

        System.out.println("JMC - calling client.initiateMultipartUpload");
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);
        System.out.println("JMC - calling client.UploadPartRequest 1");
        MultipartPartETag mp1 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1,
                new InputStreamSegment(is1, 0, fiveKB)));
        System.out.println("JMC - calling client.UploadPartRequest 2");
        MultipartPartETag mp2 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 2,
                new InputStreamSegment(is2, 0, fiveKB)));
        System.out.println("JMC - calling client.UploadPartRequest 3");
        MultipartPartETag mp3 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 3,
                new InputStreamSegment(is3, 0, fiveKB)));

        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        parts.add(mp2);
        parts.add(mp3);
        System.out.println("JMC - calling client.completeMultipartUpload");
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        CompleteMultipartUploadResult completionResult = client.completeMultipartUpload(completionRequest);
        System.out.println("JMC - returned from client.completeMultipartUpload");
    }

    @Test
    public void testSingleMultipartUploadSimple() throws Exception {
        String key = "TestObject_" + UUID.randomUUID();
        int fiveMB = 5 * 1024 * 1024;
        byte[] content = new byte[11 * 1024 * 1024];
        new Random().nextBytes(content);
        InputStream is1 = new ByteArrayInputStream(content, 0, fiveMB);
        InputStream is2 = new ByteArrayInputStream(content, fiveMB, fiveMB);
        InputStream is3 = new ByteArrayInputStream(content, 2 * fiveMB, content.length - (2 * fiveMB));

        System.out.println("JMC - calling client.initiateMultipartUpload");
        String uploadId = client.initiateMultipartUpload(getTestBucket(), key);
        System.out.println("JMC - calling client.UploadPartRequest 1");
        MultipartPartETag mp1 =  client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 1,
                new InputStreamSegment(is1, 0, fiveMB)));
        MultipartPartETag mp2 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 2,
                new InputStreamSegment(is2, 0, fiveMB)));
        System.out.println("JMC - calling client.UploadPartRequest 3");
        /*
        MultipartPart mp3 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 3,
                new InputStreamSegment(is3, 0, content.length - (2 * fiveMB))));
*/

        MultipartPartETag mp3 = client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, 3,
                new InputStreamSegment(is3, 0, 2*fiveMB)));
        SortedSet<MultipartPartETag> parts = new TreeSet<MultipartPartETag>();
        parts.add(mp1);
        parts.add(mp2);
        parts.add(mp3);
        System.out.println("JMC - calling client.completeMultipartUpload");
        CompleteMultipartUploadRequest completionRequest = new CompleteMultipartUploadRequest(getTestBucket(), key, uploadId);
        completionRequest.setParts(parts);
        CompleteMultipartUploadResult completionResult = client.completeMultipartUpload(completionRequest);
        System.out.println("JMC - returned from client.completeMultipartUpload");
    }

    protected void uploadMultipartFileParts(String bucket, String uploadId, String key, String fileName) throws Exception {
        System.out.println("JMC Entered this.uploadMultipartFileParts");
        File fileObj = new File(fileName);
        long fileLength = fileObj.length();
        int partNum = 1; //the UploadPartRequest partNum is 1 based, not 0 based
        long segmentOffset = 0;
        //long segmentLen = 5*1024*1024;
        long segmentLen = 5*1024;
        long sendLen = segmentLen;
        long totalLengthSent = 0;
        //InputStreamSegment(InputStream inputStream, long offset, long length)
        System.out.println("JMC about to loop over parts fileLength: " + fileLength + "\tsegmentLen: " + segmentLen);
        /*
        while(segmentOffset < fileLength) {
            System.out.println("JMC about to upload part: " + partNum + "\tuploadId: " + uploadId + "\tkey: " + key);
            client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, partNum,
                    new InputStreamSegment(new FileInputStream(fileName), segmentOffset, sendLen)));
            totalLengthSent += segmentLen;
            partNum++;
            segmentOffset += segmentLen;
            if (fileLength-totalLengthSent < segmentLen) {
                sendLen = fileLength-totalLengthSent;
            }
            System.out.println("JMC - uploaded multipart segment");
        }
        */
        client.uploadPart(new UploadPartRequest(getTestBucket(), key, uploadId, partNum,
                new InputStreamSegment(new FileInputStream(fileName), segmentOffset, fileLength)));
        System.out.println("JMC - finished uploading parts of multipart upload of file key: " + key);
    }

    @Test
    public void testUpdateObject() throws Exception {
        String fileName = System.getProperty("user.home") + File.separator + "test.properties";
        //create the initial object
        client.putObject(getTestBucket(), "testObject1", fileName, "text/plain");
        l4j.debug("JMC testCreateObject [1] seemed to succeed. Need to list objects for verification!!!!!!!!!!!!!!!");
 
        //TODO figure out this Range class thing
        //client.updateObject(getTestBucket(), "testObect1", range, content);
    }

    @Test
    public void testPutObject() throws Exception {
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        String key = "objectKey";
        PutObjectRequest<String> request = new PutObjectRequest<String>(getTestBucket(), key, fileName);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);
        l4j.debug("JMC - Seemed to succeed");

        ListObjectsResult result = client.listObjects(getTestBucket());
        List<S3Object> objList = result.getObjects();
        Assert.assertEquals("Failed to retrieve the object that was PUT", 1, objList.size());
        Assert.assertEquals("FAIL - name key is different", key, objList.get(0).getKey());
        l4j.debug("JMC - Success");
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

    @Ignore // TODO: blocked by STORAGE-374 and STORAGE-3674
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
        client.putObject(new PutObjectRequest<String>(getTestBucket(), key1, content).withObjectMetadata(objectMetadata));
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
        client.putObject(new PutObjectRequest<String>(getTestBucket(), key, content).withObjectMetadata(objectMetadata));
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
        l4j.debug("JMC Entered testVerifyRead");
        String fileName = System.getProperty("user.home") + File.separator + "test.properties";;
        String key = "objectKey";
        PutObjectRequest<String> request = new PutObjectRequest<String>(getTestBucket(), key, fileName);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);
        l4j.debug("JMC - successfully created the test object. will read object");

        String content = client.readObject(getTestBucket(), key, String.class);
        l4j.debug("JMC - readObject seemed to succeed. Will confirm the object contest");
        Assert.assertEquals("Wring object content", fileName, content);
        l4j.debug("JMC content: " + content);

        l4j.debug("JMC - Success");
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
            PutObjectRequest<InputStream> request = new PutObjectRequest<InputStream>(bucket3, key1, inputStream);
            request.setObjectMetadata(new S3ObjectMetadata().withContentLength(size1));
            client.putObject(request);
            Assert.assertEquals(size1, client.readObject(bucket3, key1, byte[].class).length);

            inputStream = client.readObjectStream(getTestBucket(), key2, null);
            request = new PutObjectRequest<InputStream>(bucket3, key2, inputStream);
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
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        String key = "objectKey";
        PutObjectRequest<String> request = new PutObjectRequest<String>(getTestBucket(), key, fileName);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);
        l4j.debug("JMC - successfully created the test object. will read object");

        Range range = new Range((long) 0, (long) (fileName.length() / 2));
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
        GetObjectResult<String> result = client.getObject(request,String.class);
        l4j.debug("JMC returned from client.getObject");
        l4j.debug("JMC getObject = " + result.getObject());
        S3ObjectMetadata meta = result.getObjectMetadata();
        l4j.debug("JMC meta.getContentLength(): " + meta.getContentLength());
    }
    
    //TODO
    @Test
    public void testDeleteVersion() throws Exception {
        //void deleteVersion(String bucketName, String key, String versionId);

    }
    
    @Test
    public void testDeleteObjectsRequest() throws Exception {
        //int numObjects = 5;
        //this.createTestObjects(getTestBucket(), "delObjPrefex", numObjects);

        String testObject1 = "/objectPrefix/testObject1";
        String testObject2 = "/objectPrefix/testObject2";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject1, fileName, "text/plain");
        client.putObject(getTestBucket(), testObject2, fileName, "text/plain");
  
        DeleteObjectsRequest request = new DeleteObjectsRequest(getTestBucket())
            .withKeys(testObject1, testObject2);      
        DeleteObjectsResult results = client.deleteObjects(request);
        List<AbstractDeleteResult> resultList = results.getResults();
        Assert.assertEquals(2, resultList.size());
        for(AbstractDeleteResult result: resultList) {
            System.out.println("deleteResult.key: " + result.getKey());
            if (result instanceof DeleteError ) {
                this.inspectDeleteError((DeleteError)result);
            }
            else {
                this.inspectDeleteSuccess((DeleteSuccess)result);
            }
        }
    } 
    protected void inspectDeleteError(DeleteError deleteResult) {
        System.out.println("JMC - Entered inspectDeleteResult - DeleteError");
        Assert.assertNotNull(deleteResult);
        System.out.println("deleteResult.code: " + deleteResult.getCode());
        System.out.println("deleteResult.message: " + deleteResult.getMessage());
    }
    protected void inspectDeleteSuccess(DeleteSuccess deleteResult) {
        System.out.println("JMC - Entered inspectDeleteResult - DeleteSuccess");
        System.out.println("deleteResult.deleteMarker: " + deleteResult.getDeleteMarker());
        System.out.println("deleteResult.deleteMarkerVersionId: " + deleteResult.getDeleteMarkerVersionId());
    }
    
    //S3ObjectMetadata getObjectMetadata(String bucketName, String key);
    @Test
    public void testGetObjectMetadata() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject, fileName, "text/plain");
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(getTestBucket(), testObject);
        this.validateMetadataValues(objectMetadata);
    }
    
    @Test
    public void testGetObjectMetadataRequest() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject, fileName, "text/plain");
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(getTestBucket(), testObject);
        S3ObjectMetadata objectMetadata = client.getObjectMetadata(request);
        this.validateMetadataValues(objectMetadata);
    }
    
    protected void validateMetadataValues(S3ObjectMetadata objectMetadata) throws Exception {
        Assert.assertNotNull(objectMetadata);
        System.out.println("objectMetadata.getContentType(): " + objectMetadata.getContentType());
        System.out.println("objectMetadata.getContentLength(): " + objectMetadata.getContentLength());
        System.out.println("objectMetadata.getLastModified(): " + objectMetadata.getLastModified());
        System.out.println("objectMetadata.getETag(): " + objectMetadata.getETag());
        System.out.println("objectMetadata.getContentMd5(): " + objectMetadata.getContentMd5());
        System.out.println("objectMetadata.getContentDisposition(): " + objectMetadata.getContentDisposition());
        System.out.println("objectMetadata.getContentEncoding(): " + objectMetadata.getContentEncoding());
        System.out.println("objectMetadata.getCacheControl(): " + objectMetadata.getCacheControl());
        System.out.println("objectMetadata.getHttpExpires(): " + objectMetadata.getHttpExpires());
        System.out.println("objectMetadata.getVersionId(): " + objectMetadata.getVersionId());
        System.out.println("objectMetadata.getExpirationDate(): " + objectMetadata.getExpirationDate());
        System.out.println("objectMetadata.getExpirationRuleId(): " + objectMetadata.getExpirationRuleId());
        System.out.println("printing the " + objectMetadata.getUserMetadata().size() + " user meta data key/value pairs");
        for (String userMetaKey : objectMetadata.getUserMetadata().keySet()) {
            System.out.println("user meta Key: " + userMetaKey + "\tvalue: " + objectMetadata.userMetadata(userMetaKey));
        }
    }
    
    @Test
    public void testSetObjectAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject, fileName, "text/plain");
        AccessControlList acl = this.createAcl();
        client.setObjectAcl(getTestBucket(), testObject, acl);
        this.getAndVerifyObjectAcl(getTestBucket(), testObject, acl);
    }
    
    @Test
    public void testSetObjectCannedAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject, fileName, "text/plain");
        client.setObjectAcl(getTestBucket(), testObject, CannedAcl.BucketOwnerFullControl);
        //TODO - need to validate this against a real acl
    }
    
    @Test
    public void testSetObjectAclRequestAcl() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject, fileName, "text/plain");
        AccessControlList acl = this.createAcl();
        SetObjectAclRequest request = new SetObjectAclRequest(getTestBucket(), testObject);
        l4j.debug("JMC calling request.setAcl");
        request.setAcl(acl);
        client.setObjectAcl(request);
        l4j.debug("JMC the object acl has been set. About to verify that the same acl is retrieved");
        this.getAndVerifyObjectAcl(getTestBucket(), testObject, acl);
    }
    

    @Test
    public void testSetObjectAclRequestCanned() throws Exception {
        String testObject = "/objectPrefix/testObject1";
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        client.putObject(getTestBucket(), testObject, fileName, "text/plain");
        SetObjectAclRequest request = new SetObjectAclRequest(getTestBucket(), testObject);
        request.setCannedAcl(CannedAcl.BucketOwnerFullControl);
        client.setObjectAcl(request);
        //TODO - need to verify the returned acl is comparable to the canned acl
    }
    
    
    protected void getAndVerifyObjectAcl(String bucketName, String key, AccessControlList originalAcl) throws Exception {
        l4j.debug("JMC Entered getAndVerifyObjectAcl");
        AccessControlList responseAcl = client.getObjectAcl(bucketName, key);
        l4j.debug("JMC retrieved the response object acl for verification purposes");
        this.testAclsEqual(originalAcl, responseAcl);
    }
    
    //AccessControlList getObjectAcl(String bucketName, String key);
    //tested in the set acl tests
    
    
    
    protected List<URI> parseUris(String uriString) throws Exception {
        List<URI> uris = new ArrayList<URI>();
        for (String uri : uriString.split(",")) {
            uris.add(new URI(uri));
        }
        return uris;
    }
}
