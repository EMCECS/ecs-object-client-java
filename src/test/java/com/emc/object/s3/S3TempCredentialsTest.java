package com.emc.object.s3;

import com.emc.object.Method;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.emc.object.util.TestProperties;
import com.emc.util.TestConfig;
import org.junit.jupiter.api.*;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

public class S3TempCredentialsTest extends S3JerseyClientTest {
    private static final String SESSION_TOKEN = "Cghuc190ZXN0MRIIaWFtX3VzZXIaFEFST0EzQjFGMDc0OUJFQkIzRDlFIiB1cm46ZWNzOmlhbTo6bnNfdGVzdDE6cm9sZS9yb2xlMSoUQVNJQUI1MTEzMzYwN0FBNzg1QjUyUE1hc3RlcktleVJlY29yZC0zZGE0ZTJlNmMyMGNiMzg2NDVlZTJlYjlkNWUxYzUxODJiYTBhYjQ3NWIxMDg4YWE5NDBmMzIyZTAyNWEzY2Q1OKXTrK2VL1IMZWNzLXN0cy10ZW1waL_l44QG";

    @Override
    protected S3Config createS3Config() throws Exception {
        Properties props = TestConfig.getProperties();

        String accessKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_TEMP_ACCESS_KEY);
        String secretKey = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_TEMP_SECRET_KEY);
        String securityToken = TestConfig.getPropertyNotEmpty(props, TestProperties.S3_SECURITY_TOKEN);

        S3Config s3Config = super.createS3Config();
        s3Config.withIdentity(accessKey).withSecretKey(secretKey).withSessionToken(securityToken);

        return s3Config;
    }

    @BeforeEach
    public void versionCheck() {
        Assumptions.assumeTrue(ecsVersion != null && ecsVersion.compareTo("3.6.2") >= 0, "ECS version must be at least 3.6.2");
    }

    @Test
    public void testPreSignedUrl() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl("johnsmith", "photos/puppy.jpg", new Date(1175139620000L));
        Assertions.assertEquals("http://10.246.153.111:9020/johnsmith/photos/puppy.jpg" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=sEx2C%2Bc0qiY9kXF9KkQfY%2FjelLI%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());

        // test real GET
        String key = "pre-signed-get-test", content = "This is my test object content";
        client.putObject(getTestBucket(), key, content, "text/plain");

        url = client.getPresignedUrl(getTestBucket(), key, new Date(System.currentTimeMillis() + 100000));

        Response response = ClientBuilder.newClient().target(url.toURI()).request().get(Response.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(content, response.readEntity(String.class));
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

        Assertions.assertEquals("http://10.246.153.111:9020/static.johnsmith.net/db-backup.dat.gz" +
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
        ClientBuilder.newClient().target(url.toURI())
                .request("application/x-download").header("x-amz-meta-foo", "bar")
                .put(Entity.text(content));
        Assertions.assertEquals(content, client.readObject(getTestBucket(), key, String.class));
        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assertions.assertEquals("bar", metadata.getUserMetadata("foo"));
    }

    @Test
    public void testPreSignedPutNoContentType() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L)));
        Assertions.assertEquals("http://10.246.153.111:9020/static.johnsmith.net/db-backup.dat.gz" +
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
        Assertions.assertEquals(200, con.getResponseCode());

        Assertions.assertArrayEquals(new byte[0], client.readObject(getTestBucket(), key, byte[].class));

        S3ObjectMetadata metadata = client.getObjectMetadata(getTestBucket(), key);
        Assertions.assertEquals("bar", metadata.getUserMetadata("foo"));
    }

    @Test
    public void testPreSignedUrlWithChinese() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl("test-bucket", "解析依頼C1B068.txt", new Date(1500998758000L));
        Assertions.assertEquals("http://10.246.153.111:9020/test-bucket/%E8%A7%A3%E6%9E%90%E4%BE%9D%E9%A0%BCC1B068.txt?" +
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
        Assertions.assertEquals("http://10.246.153.111:9020/johnsmith/photos/puppy.jpg" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=qdJYvXmX12mrlbJoiJ3aV2%2BsDxM%3D" +
                        "&" + S3Constants.AMZ_SECURITY_TOKEN + "=" + SESSION_TOKEN,
                url.toString());
    }

    @Disabled
    @Test
    public void testMultipleVdcs() {
    }

    @Disabled
    @Test
    public void testMpuAbortInMiddle() {
    }

    @Disabled
    @Test
    public void testSetObjectAclRequestAcl() {
    }

    @Disabled
    @Test
    public void testSetObjectAcl() {
    }

    @Disabled
    @Test
    public void testCreateFilesystemBucket() {
    }

    @Disabled
    @Test
    public void testSetBucketAclCanned() {
    }

    @Disabled
    @Test
    public void testSetGetBucketAcl() {
    }

    @Disabled
    @Test
    public void testExtendObjectRetentionPeriod() {
    }

    private S3Client getPresignDummyClient() throws URISyntaxException {
        return new S3JerseyClient(new S3Config(new URI("http://10.246.153.111:9020"))
                .withIdentity("ASIAB51133607AA785B5").withSecretKey("rhkMxcjRq6iaW1KHAdy1QuO9Qi_LCDX9cuk3XUvsgkc")
                .withSessionToken(SESSION_TOKEN));
    }
}
