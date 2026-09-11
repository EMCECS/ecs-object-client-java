package com.emc.object.s3;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.emc.object.Method;
import com.emc.object.s3.bean.BucketPolicy;
import com.emc.object.s3.bean.BucketPolicyAction;
import com.emc.object.s3.bean.BucketPolicyStatement;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.emc.object.util.TestProperties;
import com.emc.util.TestConfig;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

public class S3TempCredentialsTest extends S3JerseyClientTest {
    private static final Logger log = LoggerFactory.getLogger(S3TempCredentialsTest.class);

    // hardcoded session token used only for pre-signed URL signature verification (unit-test style)
    private static final String SESSION_TOKEN = "Cghuc190ZXN0MRIIaWFtX3VzZXIaFEFST0EzQjFGMDc0OUJFQkIzRDlFIiB1cm46ZWNzOmlhbTo6bnNfdGVzdDE6cm9sZS9yb2xlMSoUQVNJQUI1MTEzMzYwN0FBNzg1QjUyUE1hc3RlcktleVJlY29yZC0zZGE0ZTJlNmMyMGNiMzg2NDVlZTJlYjlkNWUxYzUxODJiYTBhYjQ3NWIxMDg4YWE5NDBmMzIyZTAyNWEzY2Q1OKXTrK2VL1IMZWNzLXN0cy10ZW1waL_l44QG";

    private static final String IAM_USERNAME = "obj-client-temp-cred-test-user";
    private static final String IAM_ROLE_NAME = "obj-client-temp-cred-test-role";

    // shared across tests (set up once via @BeforeClass, torn down via @AfterClass)
    private static AmazonIdentityManagement iamClient;
    private static AWSSecurityTokenService stsClient;
    private static User iamUser;
    private static Role iamRole;
    private static String stsEndpoint;
    private static String iamEndpoint;
    private static String s3AccessKey;
    private static String s3SecretKey;
    private static boolean dynamicMode;

    @BeforeClass
    public static void setupStsInfrastructure() throws Exception {
        Properties props = TestConfig.getProperties();

        stsEndpoint = props.getProperty(TestProperties.STS_ENDPOINT);
        iamEndpoint = props.getProperty(TestProperties.IAM_ENDPOINT);
        s3AccessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_ACCESS_KEY);
        s3SecretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECRET_KEY);

        if (stsEndpoint != null && !stsEndpoint.isEmpty()
                && iamEndpoint != null && !iamEndpoint.isEmpty()) {
            dynamicMode = true;

            // disable SSL validation for lab systems with self-signed certificates
            System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");

            // create IAM client
            iamClient = AmazonIdentityManagementClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(iamEndpoint, "us-east-1"))
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(s3AccessKey, s3SecretKey)))
                    .build();

            // create or reuse IAM user
            try {
                iamUser = iamClient.createUser(new CreateUserRequest(IAM_USERNAME)).getUser();
                log.info("Created IAM user: {}", iamUser.getArn());
            } catch (EntityAlreadyExistsException e) {
                iamUser = iamClient.getUser(new GetUserRequest().withUserName(IAM_USERNAME)).getUser();
                log.info("Reusing existing IAM user: {}", iamUser.getArn());
            }

            // create or reuse IAM role with AssumeRole trust policy
            String trustPolicy = "{ \"Version\": \"2012-10-17\",\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Action\": \"sts:AssumeRole\"," +
                    "      \"Resource\": \"*\",\n" +
                    "      \"Principal\": { \"AWS\": \"" + iamUser.getArn().split(":user/")[0] + ":root\" },\n" +
                    "      \"Effect\": \"Allow\"\n" +
                    "    }\n" +
                    "  ]\n }";
            try {
                iamRole = iamClient.createRole(new CreateRoleRequest()
                        .withRoleName(IAM_ROLE_NAME)
                        .withAssumeRolePolicyDocument(trustPolicy)).getRole();
                log.info("Created IAM role: {}", iamRole.getArn());
            } catch (EntityAlreadyExistsException e) {
                iamRole = iamClient.getRole(new GetRoleRequest().withRoleName(IAM_ROLE_NAME)).getRole();
                log.info("Reusing existing IAM role: {}", iamRole.getArn());
            }

            // create STS client
            stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(stsEndpoint, "us-east-1"))
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(s3AccessKey, s3SecretKey)))
                    .build();

            log.info("STS/IAM infrastructure ready for AssumeRole-based temp credential tests");
        } else {
            dynamicMode = false;
        }
    }

    @AfterClass
    public static void cleanupStsInfrastructure() {
        if (iamClient != null) {
            try {
                if (iamUser != null) {
                    // delete access keys first
                    for (AccessKeyMetadata keyMeta : iamClient.listAccessKeys(
                            new ListAccessKeysRequest().withUserName(iamUser.getUserName())).getAccessKeyMetadata()) {
                        iamClient.deleteAccessKey(new DeleteAccessKeyRequest(iamUser.getUserName(), keyMeta.getAccessKeyId()));
                    }
                    iamClient.deleteUser(new DeleteUserRequest(iamUser.getUserName()));
                    log.info("Deleted IAM user: {}", iamUser.getUserName());
                }
            } catch (Exception e) {
                log.warn("Failed to delete IAM user: {}", e.getMessage());
            }
            try {
                if (iamRole != null) {
                    iamClient.deleteRole(new DeleteRoleRequest().withRoleName(iamRole.getRoleName()));
                    log.info("Deleted IAM role: {}", iamRole.getRoleName());
                }
            } catch (Exception e) {
                log.warn("Failed to delete IAM role: {}", e.getMessage());
            }
        }
    }

    @Override
    protected S3Config createS3Config() throws Exception {
        S3Config s3Config = super.createS3Config();

        if (dynamicMode) {
            // dynamic mode: AssumeRole via STS at runtime (like ECS Sync's EcsS3Test)
            AssumeRoleResult assumeRoleResult = stsClient.assumeRole(new AssumeRoleRequest()
                    .withRoleSessionName("obj-client-temp-cred-test")
                    .withRoleArn(iamRole.getArn()));
            Credentials stsCredentials = assumeRoleResult.getCredentials();

            log.info("STS AssumeRole succeeded - using dynamic temporary credentials");
            log.info("Temp accessKeyId={}...", stsCredentials.getAccessKeyId().substring(0,
                    Math.min(8, stsCredentials.getAccessKeyId().length())));

            s3Config.withIdentity(stsCredentials.getAccessKeyId())
                    .withSecretKey(stsCredentials.getSecretAccessKey())
                    .withSessionToken(stsCredentials.getSessionToken());
        } else {
            // fallback: use static credentials from properties (legacy behavior)
            Properties props = TestConfig.getProperties();
            String accessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_TEMP_ACCESS_KEY);
            String secretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_TEMP_SECRET_KEY);
            String securityToken = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECURITY_TOKEN);

            s3Config.withIdentity(accessKey).withSecretKey(secretKey).withSessionToken(securityToken);
        }

        return s3Config;
    }

    @Override
    protected void createBucket(String bucketName) throws Exception {
        // bucket must be created with the original (non-temp) credentials because
        // the AssumeRole temp credentials may not have permission to create buckets.
        // Then we set a bucket policy granting the assumed role access.
        S3Config ownerConfig = s3ConfigFromProperties();
        S3Client ownerClient = new S3JerseyClient(ownerConfig);
        try {
            ownerClient.createBucket(bucketName);
            this.bucketOwner = ownerClient.getBucketAcl(bucketName).getOwner();

            if (dynamicMode) {
                // grant the assumed role full access to this bucket (like ECS Sync's EcsS3Test)
                BucketPolicy bucketPolicy = new BucketPolicy()
                        .withVersion("2012-10-17")
                        .withId("temp-cred-test-policy")
                        .withStatements(Arrays.asList(
                                new BucketPolicyStatement()
                                        .withSid("role-object-access")
                                        .withPrincipal("{\"AWS\":\"" + iamRole.getArn() + "\"}")
                                        .withEffect(BucketPolicyStatement.Effect.Allow)
                                        .withActions(BucketPolicyAction.All)
                                        .withResource("arn:aws:s3:::" + bucketName + "/*"),
                                new BucketPolicyStatement()
                                        .withSid("role-bucket-access")
                                        .withPrincipal("{\"AWS\":\"" + iamRole.getArn() + "\"}")
                                        .withEffect(BucketPolicyStatement.Effect.Allow)
                                        .withActions(BucketPolicyAction.All)
                                        .withResource("arn:aws:s3:::" + bucketName)
                        ));
                ownerClient.setBucketPolicy(bucketName, bucketPolicy);
                log.info("Set bucket policy for role {} on bucket {}", iamRole.getArn(), bucketName);
            }
        } finally {
            ownerClient.destroy();
        }
    }

    @Override
    protected void cleanUpBucket(String bucketName) {
        // clean up with owner credentials (temp creds may not have delete-bucket permission)
        try {
            S3Config ownerConfig = s3ConfigFromProperties();
            S3Client ownerClient = new S3JerseyClient(ownerConfig);
            try {
                if (ownerClient.bucketExists(bucketName)) {
                    if (ownerClient.getBucketVersioning(bucketName).getStatus() != null) {
                        for (com.emc.object.s3.bean.AbstractVersion version :
                                ownerClient.listVersions(new com.emc.object.s3.request.ListVersionsRequest(bucketName)
                                        .withEncodingType(com.emc.object.s3.bean.EncodingType.url)).getVersions()) {
                            ownerClient.deleteObject(new com.emc.object.s3.request.DeleteObjectRequest(bucketName, version.getKey())
                                    .withVersionId(version.getVersionId()));
                        }
                    } else {
                        for (com.emc.object.s3.bean.S3Object object :
                                ownerClient.listObjects(new com.emc.object.s3.request.ListObjectsRequest(bucketName)
                                        .withEncodingType(com.emc.object.s3.bean.EncodingType.url)).getObjects()) {
                            ownerClient.deleteObject(bucketName, object.getKey());
                        }
                    }
                    ownerClient.deleteBucket(bucketName);
                }
            } finally {
                ownerClient.destroy();
            }
        } catch (Exception e) {
            log.warn("Failed to clean up bucket {}: {}", bucketName, e.getMessage());
        }
    }

    @Before
    public void versionCheck() {
        Assume.assumeTrue("ECS version must be at least 3.6.2", ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0);
    }

    @Test
    public void testPreSignedUrl() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl("johnsmith", "photos/puppy.jpg", new Date(1175139620000L));
        Assert.assertEquals("http://10.246.153.111:9020/johnsmith/photos/puppy.jpg" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=sEx2C%2Bc0qiY9kXF9KkQfY%2FjelLI%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());

        // test real GET
        String key = "pre-signed-get-test", content = "This is my test object content";
        client.putObject(getTestBucket(), key, content, "text/plain");

        url = client.getPresignedUrl(getTestBucket(), key, new Date(System.currentTimeMillis() + 100000));

        javax.ws.rs.core.Response response = javax.ws.rs.client.ClientBuilder.newClient().target(url.toURI()).request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(content, response.readEntity(String.class));
    }

    @Test
    public void testPreSignedPutUrl() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L))
                        .withObjectMetadata(new S3ObjectMetadata().withContentType("application/x-download")
                                .withContentMd5("4gJE4saaMU4BqNR0kLY+lw==")
                                .addUserMetadata("checksumalgorithm", "crc32")
                                .addUserMetadata("filechecksum", "0x02661779")
                                .addUserMetadata("reviewedby", "joe@johnsmith.net,jane@johnsmith.net"))
        );

        Assert.assertEquals("http://10.246.153.111:9020/static.johnsmith.net/db-backup.dat.gz" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=llrkH6%2BoAuzr6F71RD0xsyUqOFY%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());

        // test real PUT
        String key = "pre-signed-put-test", content = "This is my test object content";
        url = client.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, getTestBucket(), key, new Date(System.currentTimeMillis() + 100000))
                        .withObjectMetadata(new S3ObjectMetadata().withContentType("application/x-download")
                                .addUserMetadata("foo", "bar"))
        );
        javax.ws.rs.client.ClientBuilder.newClient().target(url.toURI())
                .request().header("Content-Type", "application/x-download").header("x-amz-meta-foo", "bar")
                .put(javax.ws.rs.client.Entity.entity(content, "application/x-download"));
        Assert.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assert.assertEquals("bar", metadata.getUserMetadata("foo"));
    }

    @Test
    public void testPreSignedPutNoContentType() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L)));
        Assert.assertEquals("http://10.246.153.111:9020/static.johnsmith.net/db-backup.dat.gz" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=Z4JSBg7EHfIGgZeix0YNmy0XQEI%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());

        // test real PUT
        // only way is to use HttpURLConnection directly
        String key = "pre-signed-put-test-2";
        url = client.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, getTestBucket(), key, new Date(System.currentTimeMillis() + 100000))
                        .withObjectMetadata(new S3ObjectMetadata().addUserMetadata("foo", "bar")));

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
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl("test-bucket", "解析依頼C1B068.txt", new Date(1500998758000L));
        Assert.assertEquals("http://10.246.153.111:9020/test-bucket/%E8%A7%A3%E6%9E%90%E4%BE%9D%E9%A0%BCC1B068.txt?" +
                        "AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1500998758" +
                        "&Signature=9JowVXKUdWD43PsmtCa%2BeYkkYL0%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());
    }

    @Test
    public void testPreSignedUrlWithHeaders() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(
                        Method.PUT, "johnsmith", "photos/puppy.jpg", new Date(1175139620000L))
                        .withObjectMetadata(
                                new S3ObjectMetadata().withContentType("image/jpeg")
                                        .withContentMd5("4gJE4saaMU4BqNR0kLY+lw==")));
        Assert.assertEquals("http://10.246.153.111:9020/johnsmith/photos/puppy.jpg" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=qdJYvXmX12mrlbJoiJ3aV2%2BsDxM%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());
    }

    @Ignore
    @Test
    public void testMultipleVdcs() {
    }

    @Ignore
    @Test
    public void testMpuAbortInMiddle() {
    }

    @Ignore
    @Test
    public void testSetObjectAclRequestAcl() {
    }

    @Ignore
    @Test
    public void testSetObjectAcl() {
    }

    @Ignore
    @Test
    public void testCreateFilesystemBucket() {
    }

    @Ignore
    @Test
    public void testSetBucketAclCanned() {
    }

    @Ignore
    @Test
    public void testSetGetBucketAcl() {
    }

    @Ignore
    @Test
    public void testExtendObjectRetentionPeriod() {
    }

    // bucket-admin operations not allowed with AssumeRole temp credentials
    // (the bucket policy only grants access to object operations on the test bucket)
    @Ignore("temp credentials cannot list buckets at account level")
    @Test
    public void testListBuckets() {
    }

    @Ignore("temp credentials cannot list buckets at account level")
    @Test
    public void testListBucketsReq() {
    }

    @Ignore("temp credentials cannot get bucket info")
    @Test
    public void testGetBucketInfo() {
    }

    @Ignore("temp credentials cannot create new buckets")
    @Test
    public void testCreateBucketRequest() {
    }

    @Ignore("temp credentials cannot create encrypted buckets")
    @Test
    public void testCreateEncryptedBucket() {
    }

    @Ignore("temp credentials cannot create stale-read-allowed buckets")
    @Test
    public void testCreateStaleReadAllowedBucket() {
    }

    @Ignore("temp credentials cannot delete buckets")
    @Test
    public void testDeleteBucket() {
    }

    @Ignore("temp credentials cannot delete buckets with background tasks")
    @Test
    public void testDeleteBucketWithBackgroundTasks() {
    }

    @Ignore("temp credentials cannot delete buckets with MPU background tasks")
    @Test
    public void testDeleteBucketWithMPUWithBackgroundTasks() {
    }

    @Ignore("temp credentials cannot set bucket policy")
    @Test
    public void testBucketPolicy() {
    }

    @Ignore("temp credentials cannot create encrypted buckets")
    @Test
    public void testUploadPartChecksumOnEncryptedBucket() {
    }

    @Ignore("temp credentials cannot create buckets in other namespaces")
    @Test
    public void testStreamObjectBetweenBuckets() {
    }

    @Ignore("IAM user is not supported for Copy Range API on ECS")
    @Test
    public void testCopyRangeAPI() {
    }

    private S3Client getPresignDummyClient() throws URISyntaxException {
        return new S3JerseyClient(new S3Config(new URI("http://10.246.153.111:9020"))
                .withIdentity("ASIAB51133607AA785B5").withSecretKey("rhkMxcjRq6iaW1KHAdy1QuO9Qi_LCDX9cuk3XUvsgkc")
                .withSessionToken(SESSION_TOKEN));
    }
}
