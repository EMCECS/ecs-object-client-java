package com.emc.object.s3;

//import com.amazonaws.services.s3.model.CORSRule;

import com.emc.object.AbstractClientTest;
import com.emc.object.s3.bean.*;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.ListObjectsRequest;
import com.emc.object.s3.request.ListVersionsRequest;
import com.emc.object.s3.request.SetBucketAclRequest;
import com.emc.vipr.services.lib.ViprConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.*;

//import com.emc.object.s3.bean.Owner;
//JMC added

public class S3JerseyClientTest extends AbstractClientTest {
    protected S3Client client;
    protected String  secretKey;
    
    @Rule
    public TestRule watcher = new TestWatcher() {
    	protected void starting(Description description) {
    		System.out.println("Starting test:>>>>>>>>>>>>>>> " + description.getMethodName());
    	}
    };
   
    @Override
    protected String getTestBucketPrefix() {
        return "s3-client-test";
    }

    @Override
    protected void createBucket(String bucketName) throws Exception {
        client.createBucket(bucketName);
    }

    @Override
    protected void cleanUpBucket(String bucketName) throws Exception {
		for (S3Object object : client.listObjects(getTestBucket()).getObjects()) {
			client.deleteObject(bucketName, object.getKey());
		}
		client.deleteBucket(bucketName);
    }
    
    /*
    protected String createBucketAndName() throws Exception {
    	String bucketName = "TestBucket_" + UUID.randomUUID();
    	String msg="Bucket could NOT be created";
    	createBucket(bucketName);
    	Assert.assertTrue(msg, client.bucketExists(bucketName));
    	return bucketName;
    }
*/
    @Override
    public void initClient() throws Exception {
        Properties props = ViprConfig.getProperties();

        String accessKey = ViprConfig.getPropertyNotEmpty(props, ViprConfig.PROP_S3_ACCESS_KEY_ID);
        this.secretKey = ViprConfig.getPropertyNotEmpty(props, ViprConfig.PROP_S3_SECRET_KEY);
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
    	//String bucketName = createBucketAndName();
        ListBucketsResult result = client.listBuckets();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOwner());
        Assert.assertNotNull(result.getBuckets());
        //cleanUpBucket(bucketName);
        System.out.println("JMC testListBuckets succeeded for user: " + result.getOwner().getId() +  " !!!!!!!!!!!!!!!!!!!");
    }
    
    @Test
    public void testListBucketsReq() {
    	//ListBucketsResult listBuckets(ListBucketsRequest request);
    }
    
    @Test
    public void testBucketExists() throws Exception {
    	Assert.assertTrue("Bucket " + getTestBucket() + " should exist but does NOT", client.bucketExists(getTestBucket()));
    	System.out.println("JMC testBucketExists succeeded!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    //this is tested by default in the @Before method
    //void createBucket(String bucketName);

    //TODO
    //void createBucket(CreateBucketRequest request);
    
    @Test
    public void testDeleteBucket() throws Exception {
    	//String bucketName = createBucketAndName();
    	client.deleteBucket(getTestBucket());
    	Assert.assertFalse("failed to delete bucket " + getTestBucket(), client.bucketExists(getTestBucket()));
    	//JMC need to note that the @After cleanup will fail
    }

    protected AccessControlList createAcl() {
    	CanonicalUser cu1 = new CanonicalUser("userId1","userDisplayName1");
    	Permission perm = Permission.FULL_CONTROL;
    	Grant grant = new Grant(cu1, perm);
    	

    	CanonicalUser cu2 = new CanonicalUser("userId2","userDisplayName2");
    	Permission perm2 = Permission.READ;
    	Grant grant2 = new Grant(cu2, perm2);
    	Set<Grant> grantSet = new HashSet<Grant>();
    	grantSet.add(grant);
    	grantSet.add(grant2);
    	
    	CanonicalUser owner = new CanonicalUser();
    	owner.setDisplayName("ownerName");
    	owner.setId("ownerId");
    	
    	AccessControlList acl = new AccessControlList();
    	acl.setOwner(owner);
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
    	CorsRule cr0 = new CorsRule();
    	CorsRule cr1 = new CorsRule();
    	CorsRule cr2 = new CorsRule();

    	ArrayList<CorsRule> crArr = new ArrayList<CorsRule>();
    	CorsConfiguration cc;
    	CorsConfiguration ccVerify;
    	List<CorsRule> crArrVerify;
    	
    	cr0.setId("corsRuleTestId0");
    	cr1.setId("corsRuleTestId1");
    	cr2.setId("corsRuleTestId2");

    	crArr.add(cr0);
    	crArr.add(cr1);
    	crArr.add(cr2);

    	cc = new CorsConfiguration();
    	cc.setCorsRules(new ArrayList<CorsRule>());
    	client.setBucketCors(getTestBucket(), cc);
    	
    	ccVerify = client.getBucketCors(getTestBucket());
    	Assert.assertNotNull("CorsConfiguration should NOT be null but is", ccVerify);
    	crArrVerify = ccVerify.getCorsRules();
    	Assert.assertNotNull("CorsRule list should NOT be null but is", crArrVerify);
    	Assert.assertEquals("There are NOT the same number of CorsRule items", crArr.size(), crArrVerify.size());
 
    	//JMC the rules might not come back in the same order! need to change
    	//maybe could brute force or look into sort or rules (based upon id maybe)
    	int testResults = 0x0;
    	for (int i=0;i<crArrVerify.size();i++) {
    		if (cr0.getId().equals(crArrVerify.get(i).getId())) {
    			//Assert.assertEquals(crArr.get(i).getId(), crArrVerify.get(i).getId());
    			testResults = testResults & 0x001;
    		}
    		if (cr0.getId().equals(crArrVerify.get(i).getId())) {
    			//Assert.assertEquals(crArr.get(i).getId(), crArrVerify.get(i).getId());
    			testResults = testResults & 0x010;
    		}
    		if (cr0.getId().equals(crArrVerify.get(i).getId())) {
    			//Assert.assertEquals(crArr.get(i).getId(), crArrVerify.get(i).getId());
    			testResults = testResults & 0x100;
    		}
    	}
    	Assert.assertEquals("Incorrect CorRules returned", testResults, 0x111);
    }

    
    @Test
    public void testDeleteBucketCors() throws Exception {
    	//String bucketName = createBucketAndName();
    	CorsRule cr = new CorsRule();
    	ArrayList<CorsRule> crArr = new ArrayList<CorsRule>();
    	CorsConfiguration cc;
    	CorsConfiguration ccVerify;
    	
    	cr.setId("corsRuleTestId");
    	crArr.add(cr);
    	cc = new CorsConfiguration();
    	cc.setCorsRules(new ArrayList<CorsRule>());
    	client.setBucketCors(getTestBucket(), cc);
    	
    	ccVerify = client.getBucketCors(getTestBucket());
    	Assert.assertNotNull("deleteBucketCors NOT tested ccVerify prereq is null", ccVerify);
    	
    	//reinitialize this object to null, so it can be used for retesting
    	ccVerify = null;
    	client.deleteBucketCors(getTestBucket());//PRIMARY TEST CALL
    	ccVerify = client.getBucketCors(getTestBucket());
    	Assert.assertNull("deleteBucketCors failed ccVerify is NOT null", ccVerify);
    	
    	//make sure the bucket still exists
    	Assert.assertTrue("deleteBucketCors succeeded, but bucket does not exist", client.bucketExists(getTestBucket()));
    	//cleanUpBucket(getTestBucket());
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
    	Assert.assertNull(client.getBucketLifecycle(bn));

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
    	System.out.println("JMC Entered testListObjectsBucketNamePrefix");
    	String myPrefix = "testPrefix";
    	int numObjects = 10;
        this.createTestObjects("testPrefix/", numObjects);
        System.out.println("JMC created all test objects. Now they will be listed");
    	ListObjectsResult result = client.listObjects(getTestBucket(), myPrefix);
    	Assert.assertNotNull("ListObjectsResult was null and should NOT be",result);
    	Assert.assertEquals("The correct number of objects were NOT returned", numObjects, result.getObjects().size());
    	System.out.println("JMC testListObjectsBucketNamePrefix succeeded. Going to print object names!!!!!");
    	System.out.println(Integer.toString(result.getObjects().size()) +" were returned");
    	//List<S3Object> s3Objects = result.getObjects();
    	int tempInt = 0;
    	for (S3Object s3Object: result.getObjects()) {
    		System.out.println("s3Object[" + Integer.toString(tempInt) + "]: " + s3Object.getKey());
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
    	ListVersionsResult lvr = client.listVersions(getTestBucket(), getTestBucketPrefix());
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
    	System.out.println("JMC Entered createTestObjects. Creating " + Integer.toString(numObjects));
    	String objectName;
    	File testFile = new File(System.getProperty("user.home") + File.separator +"vipr.properties");
        if(!testFile.exists()) {
        	throw new FileNotFoundException("vipr.properties");
        }
        
        for(int i=0; i<numObjects; i++) {
        	objectName = "TestObject_" + UUID.randomUUID();
        	System.out.println("JMC about to create " + objectName);
        	client.createObject(getTestBucket(), prefixWithDelim + objectName, testFile, "text/plain");
        	System.out.println("JMC client.createObject " + objectName + " seemed to work");
        }
        System.out.println("JMC Done creating test objects");
    }
    
    //TODO need to actually make these multi part uploads
    protected void createMultipartTestObjects(String prefix, int numObjects) throws Exception {
    	String objectName;
    	File testFile = new File(System.getProperty("user.home") + File.separator +"vipr.properties");
        if(!testFile.exists()) {
        	throw new FileNotFoundException("vipr.properties");
        }
        
        for(int i=0; i<numObjects; i++) {
        	objectName = "TestObject_" + UUID.randomUUID();
        	client.createObject(getTestBucket(), prefix + objectName, testFile, "text/plain");
        }
    }
    
    @Test 
    public void testCreateObject() throws Exception {
    	System.out.println("JMC Entered testCreateObject");
//    	File testFile = new File(System.getProperty("user.home") + File.separator +"vipr.properties");
//        if(!testFile.exists()) {
//        	throw new FileNotFoundException("vipr.properties");
//        }
		//TODO - I don't understand the the prefix and name of the object I'm creating is
		//can I specify that with this method?
        //Amazon S3 uses the File object
        //client.createObject(getTestBucket(), "/objectPrefix/testObject1", testFile, "text/plain");
        client.createObject(getTestBucket(), "/objectPrefix/testObject1", "/Users/conerj/vipr.properties", "text/plain");
        System.out.println("JMC testCreateObject [1] seemed to succeed. Need to list objects for verification!!!!!!!!!!!!!!!");

        //client.createObject(getTestBucket(), "/objectPrefix/testObject2", testFile, "text/plain");
        client.createObject(getTestBucket(), "/objectPrefix/testObject2", "/Users/conerj/vipr.properties", "text/plain");
        System.out.println("JMC testCreateObject [2] seemed to succeed. Need to list objects for verification!!!!!!!!!!!!!!!");

    }
    
    @Test
    public void testBucketLocation() throws Exception {
    	LocationConstraint lc = client.getBucketLocation(getTestBucket());
    	Assert.assertNotNull(lc);
    	System.out.println("Bucket location: " + lc.getRegion());
    }
    
    @Test
    public void testSetBucketVersioning() throws Exception {
    	VersioningConfiguration vc = new VersioningConfiguration();
    	//TODO no way to do since this enum is inner private
    	//vc.setStatus(VersioningConfiguration.Status.Enabled);
    	client.setBucketVersioning(getTestBucket(), vc);
    	
    	VersioningConfiguration vcResult = client.getBucketVersioning(getTestBucket());
    	//TODO need to assert that the vc settings are equal
    }
    
    @Test
    public void testListMultipartUploads() throws Exception {
    	int numObjects = 2;
    	String prefix = "/multiPrefix";
    	this.createMultipartTestObjects(prefix, numObjects);
    	ListMultipartUploadsResult result = client.listMultipartUploads(getTestBucket());
    	Assert.assertNotNull(result);
    	List<Upload> lu = result.getUploads();
    	Assert.assertEquals(numObjects, lu.size());
    }
    
    //TODO
    // ListMultipartUploadsResult listMultipartUploads(ListMultipartUploadsRequest request);
    
    
    
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
