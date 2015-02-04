/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

import com.emc.object.Range;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.*;
import java.net.URI;
import java.util.*;

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
        CanonicalUser cu1 = new CanonicalUser("userId1", "userDisplayName1");
        Permission perm = Permission.FULL_CONTROL;
        Grant grant = new Grant(cu1, perm);


        CanonicalUser cu2 = new CanonicalUser("userId2","userDisplayName2");
        Permission perm2 = Permission.READ;
        Grant grant2 = new Grant(cu2, perm2);
        Set<Grant> grantSet = new HashSet<>();
        grantSet.add(grant);
        grantSet.add(grant2);

        AccessControlList acl = new AccessControlList();
        acl.setGrants(grantSet);
        return acl;
    }
    
    protected void testAclsEqual(AccessControlList acl1, AccessControlList acl2) {
        CanonicalUser owner1 = acl1.getOwner();
        CanonicalUser owner2 = acl2.getOwner();
        Assert.assertEquals(owner1.getId(), owner2.getId());
        Assert.assertEquals(owner1.getDisplayName(), owner2.getDisplayName());

        Grant[] grants1 = (Grant[])acl1.getGrants().toArray();
        Grant[] grants2 = (Grant[])acl2.getGrants().toArray();
        Assert.assertEquals(grants1.length, grants2.length);
        Assert.assertTrue("The acl sets are not deeply equal", Arrays.deepEquals(grants1, grants2));
    }
    
    public void testSetBucketAcl() {
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

        ArrayList<CorsRule> crArr = new ArrayList<>();
        List<CorsRule> crArrVerify = new ArrayList<>();

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
        ArrayList<CorsRule> crArr = new ArrayList<>();

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
        ArrayList<LifecycleRule> lcrList = new ArrayList<>();
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
        if (!testFile.exists()) {
            throw new FileNotFoundException("test.properties");
        }
        
        for(int i=0; i<numObjects; i++) {
            objectName = "TestObject_" + UUID.randomUUID();
            l4j.debug("JMC about to create " + objectName);
            client.putObject(bucket, prefixWithDelim + objectName, testFile, "text/plain");
            l4j.debug("JMC client.createObject " + objectName + " seemed to work");
        }
        l4j.debug("JMC Done creating test objects");
    }

    //TODO need to actually make these multi part uploads
    protected void createMultipartTestObjects(String prefix, int numObjects) throws Exception {
        String objectName;
        File testFile = new File(System.getProperty("user.home") + File.separator +"test.properties");
        if(!testFile.exists()) {
            throw new FileNotFoundException("test.properties");
        }
        
        for(int i=0; i<numObjects; i++) {
            objectName = "TestObject_" + UUID.randomUUID();
            client.initiateMultipartUpload(getTestBucket(), prefix + objectName);
        }
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
        PutObjectRequest<String> request = new PutObjectRequest<>(getTestBucket(), "/objectPrefix/testObject1", fileName);
        PutObjectResult result = client.putObject(request);
        Assert.assertNotNull(result);
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
    public void testListMultipartUploads() throws Exception {
        int numObjects = 2;
        String prefix = "multiPrefix/";
        this.createMultipartTestObjects(prefix, numObjects);
        ListMultipartUploadsResult result = client.listMultipartUploads(getTestBucket());
        Assert.assertNotNull(result);
        List<Upload> lu = result.getUploads();
        Assert.assertEquals(numObjects, lu.size());
    }

    //TODO
    // ListMultipartUploadsResult listMultipartUploads(ListMultipartUploadsRequest request);


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
        PutObjectRequest<String> request = new PutObjectRequest<>(getTestBucket(), key, fileName);
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
    public void testVerifyRead() throws Exception {
        l4j.debug("JMC Entered testVerifyRead");
        String fileName = System.getProperty("user.home") + File.separator + "test.properties";
        String key = "objectKey";
        PutObjectRequest<String> request = new PutObjectRequest<>(getTestBucket(), key, fileName);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);
        l4j.debug("JMC - successfully created the test object. will read object");

        String content = client.readObject(getTestBucket(), key, String.class);
        l4j.debug("JMC - readObject seemed to succeed. Will confirm the object contest");
        Assert.assertEquals("Wring object content", fileName, content);

        l4j.debug("JMC - Success");
    }
  
    @Test
    public void testReadObjectStreamRange() throws Exception {
        String fileName = System.getProperty("user.home") + File.separator +"test.properties";
        String key = "objectKey";
        PutObjectRequest<String> request = new PutObjectRequest<>(getTestBucket(), key, fileName);
        request.setObjectMetadata(new S3ObjectMetadata().withContentType("text/plain"));
        client.putObject(request);
        l4j.debug("JMC - successfully created the test object. will read object");

        Range range = new Range((long) 0, (long) (fileName.length() / 2));
        InputStream is = client.readObjectStream(getTestBucket(), key, range);
        l4j.debug("JMC - readObjectStream seemed to succeed. Will confirm the object contest");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            l4j.debug(line);
        }
        l4j.debug("JMC - Success");
    }

    //<T> GetObjectResult<T> getObject(GetObjectRequest request, Class<T> objectType);
    @Test
    public void testGetObjectResultTemplate() throws Exception {

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
        request.setAcl(acl);
        client.setObjectAcl(request);
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
        AccessControlList responseAcl = client.getObjectAcl(bucketName, key);
        this.testAclsEqual(originalAcl, responseAcl);
    }
    
    //AccessControlList getObjectAcl(String bucketName, String key);
    //tested in the set acl tests
    
    
    
    protected List<URI> parseUris(String uriString) throws Exception {
        List<URI> uris = new ArrayList<>();
        for (String uri : uriString.split(",")) {
            uris.add(new URI(uri));
        }
        return uris;
    }
}
