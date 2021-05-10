package com.emc.object.s3;

import com.emc.object.Method;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.sun.jersey.api.client.Client;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

public class V2STSBasicTest extends S3JerseyClientTest{
    private static final Logger l4j = Logger.getLogger(V2STSBasicTest.class);
    private static final String SESSION_TOKEN = "Cghuc190ZXN0MRIIaWFtX3VzZXIaFEFST0EzQjFGMDc0OUJFQkIzRDlFIiB1cm46ZWNzOmlhbTo6bnNfdGVzdDE6cm9sZS9yb2xlMSoUQVNJQUI1MTEzMzYwN0FBNzg1QjUyUE1hc3RlcktleVJlY29yZC0zZGE0ZTJlNmMyMGNiMzg2NDVlZTJlYjlkNWUxYzUxODJiYTBhYjQ3NWIxMDg4YWE5NDBmMzIyZTAyNWEzY2Q1OKXTrK2VL1IMZWNzLXN0cy10ZW1waL_l44QG";

    @Override
    protected S3Config createS3Config() throws Exception {
        S3Config s3Config = s3ConfigFromProperties();
        Assume.assumeTrue("skip this test run STS instead", s3Config.getSessionToken() != null);
        return s3Config;
    }

    @Test
    public void testListDataNodes() {
        super.testListDataNodes();
    }

    @Test
    public void testListBucketsReq() {
        super.testListBucketsReq();
    }

    @Test
    public void testBucketExists() throws Exception {
        super.testBucketExists();
    }

    @Test
    public void testCreateBucketRequest() throws Exception {
        super.testCreateBucketRequest();
    }

    @Test
    public void testCreateStaleReadAllowedBucket() {
        super.testCreateStaleReadAllowedBucket();
    }

    @Test
    public void testCreateEncryptedBucket() {
        super.testCreateEncryptedBucket();
    }

    @Test
    public void testGetBucketInfo() {
        super.testGetBucketInfo();
    }

    @Test
    public void testDeleteBucket() throws Exception {
        super.testDeleteBucket();
    }

    @Test
    public void testDeleteBucketWithObjects() throws Exception {
        super.testDeleteBucketWithObjects();
    }


    @Test
    public void testSetGetBucketCors() throws Exception {
        super.testSetGetBucketCors();
    }

    @Test
    public void testDeleteBucketCors() throws Exception {
        super.testDeleteBucketCors();
    }

    @Test
    public void testBucketLifecycle() {
        super.testBucketLifecycle();
    }

    @Test
    public void testBucketPolicy() {
        super.testBucketPolicy();
    }

    @Test
    public void testListObjects() throws Exception {
        super.testListObjects();
    }

    @Test
    public void testListObjectsWithPrefix() throws Exception {
        super.testListObjectsWithPrefix();
    }

    @Test
    public void testListObjectsPagingWithEncodedDelim() {
        super.testListObjectsPagingWithEncodedDelim();
    }

    @Test
    public void testListObjectsPaging() {
        super.testListObjectsPaging();
    }

    @Test
    public void testListObjectsPagingDelim() {
        super.testListObjectsPagingDelim();
    }

    @Test
    public void testListObjectsPagingWithPrefix() {
        super.testListObjectsPagingWithPrefix();
    }

    @Test
    public void testListObjectsWithEncoding() {
        super.testListObjectsWithEncoding();
    }

    @Test
    public void testListAndReadVersions() throws Exception {
        super.testListAndReadVersions();
    }

    @Test
    public void testListVersionsPaging() {
        super.testListVersionsPaging();
    }

    @Test
    public void testListVersionsPagingPrefixDelim() throws Exception {
        super.testListVersionsPagingPrefixDelim();
    }

    @Test
    public void testReadObject() {
        super.testReadObject();
    }

    @Test
    public void testUpdateObjectWithRange() throws Exception {
        super.testUpdateObjectWithRange();
    }

    @Test
    public void testGetObjectPreconditions() {
        super.testGetObjectPreconditions();
    }

    @Test
    public void testPutObjectPreconditions() {
        super.testPutObjectPreconditions();
    }

    @Test
    public void testCreateObjectByteArray() {
        super.testCreateObjectByteArray();
    }

    @Test
    public void testCreateObjectWithStream() throws Exception {
        super.testCreateObjectWithStream();
    }

    @Test
    public void testCreateJsonObjectWithStream() {
        super.testCreateJsonObjectWithStream();
    }

    @Test
    public void testCreateObjectString() {
        super.testCreateObjectString();
    }

    @Test
    public void testCreateObjectWithRequest() {
        super.testCreateObjectWithRequest();
    }

    @Test
    public void testCreateObjectChunkedWithRequest() {
        super.testCreateObjectChunkedWithRequest();
    }

    @Test
    public void testCreateObjectWithMetadata() {
        super.testCreateObjectWithMetadata();
    }

    @Test
    public void testCreateObjectWithRetentionPeriod() throws Exception {
        super.testCreateObjectWithRetentionPeriod();
    }

    @Test
    public void testCreateObjectWithRetentionPolicy() throws Exception {
        super.testCreateObjectWithRetentionPolicy();
    }

    @Test
    public void testLargeObjectContentLength() throws Exception {
        super.testLargeObjectContentLength();
    }

    @Test
    public void testLargeFileUploader() throws Exception {
        super.testLargeFileUploader();
    }

    @Test
    public void testLargeFileUploaderProgressListener() throws Exception {
        super.testLargeFileUploaderProgressListener();
    }

    @Test
    public void testLargeFileUploaderStream() throws Exception {
        super.testLargeFileUploaderStream();
    }

    @Test
    public void testLargeFileDownloader() throws Exception {
        super.testLargeFileDownloader();
    }

    @Test
    public void testBucketLocation() throws Exception {
        super.testBucketLocation();
    }

    @Test
    public void testSetBucketVersioning() throws Exception {
        super.testSetBucketVersioning();
    }

    @Test
    public void testSingleMultipartUploadMostSimpleOnePart() throws Exception {
        super.testSingleMultipartUploadMostSimpleOnePart();
    }

    @Test
    public void testSingleMultipartUploadListParts() throws Exception {
        super.testSingleMultipartUploadListParts();
    }

    @Test
    public void testMultiThreadMultipartUploadListPartsPagination() throws Exception {
        super.testMultiThreadMultipartUploadListPartsPagination();
    }

    @Test
    public void testMultiThreadMultipartUploadMostSimple() throws Exception {
        super.testMultiThreadMultipartUploadMostSimple();
    }

    @Test
    public void testSingleMultipartUploadMostSimple() throws Exception {
        super.testSingleMultipartUploadMostSimple();
    }

    @Test
    public void testSingleMultipartUploadSimple() throws Exception {
        super.testSingleMultipartUploadSimple();
    }

    @Test
    public void testPutObject() {
        super.testPutObject();
    }

    @Test
    public void testEmptyObject() {
        super.testEmptyObject();
    }

    @Test
    public void testEmptyObjectChunked() {
        super.testEmptyObjectChunked();
    }

    @Test
    public void testPutObjectWithSpace() {
        super.testPutObjectWithSpace();
    }

    @Test
    public void testPutObjectWithPlus() {
        super.testPutObjectWithPlus();
    }

    @Test
    public void testPutObjectWithPercent() {
        super.testPutObjectWithPercent();
    }

    @Test
    public void testPutObjectWithChinese() {
        super.testPutObjectWithChinese();
    }

    @Test
    public void testPutObjectWithSmartQuote() {
        super.testPutObjectWithSmartQuote();
    }

    /**
     * Tests all the items in the java.net.URI "punct" character class.
     */
    @Test
    public void testPutObjectWithUriPunct() {
        super.testPutObjectWithUriPunct();
    }

    /**
     * Tests all the items in the java.net.URI "reserved" character class.
     */
    @Test
    public void testPutObjectWithUriReserved() {
        super.testPutObjectWithUriReserved();
    }

    @Test
    public void testPutObjectWithMd5() throws Exception {
        super.testPutObjectWithMd5();
    }

    @Test
    public void testPutObjectWithRetentionPeriod() throws Exception {
        super.testPutObjectWithRetentionPeriod();
    }

    @Test
    public void testPutObjectWithRetentionPolicy() {
        super.testPutObjectWithRetentionPolicy();
    }

    @Test
    public void testAppendObject() throws Exception {
        super.testAppendObject();
    }

    @Test
    public void testCopyObject() {
        super.testCopyObject();
    }

    @Test
    public void testCopyObjectPlusSource() {
        super.testCopyObjectPlusSource();
    }

    @Test
    public void testCopyObjectPlusDest() {
        super.testCopyObjectPlusDest();
    }

    @Test
    public void testCopyObjectPlusBoth() {
        super.testCopyObjectPlusBoth();
    }

    @Test
    public void testCopyObjectSpaceSrc() {
        super.testCopyObjectSpaceSrc();
    }

    @Test
    public void testCopyObjectSpaceDest() {
        super.testCopyObjectSpaceDest();
    }

    @Test
    public void testCopyObjectSpaceBoth() {
        super.testCopyObjectSpaceBoth();
    }

    @Test
    public void testCopyObjectChineseSrc() {
        super.testCopyObjectChineseSrc();
    }

    @Test
    public void testCopyObjectChineseDest() {
        super.testCopyObjectChineseDest();
    }

    @Test
    public void testCopyObjectChineseBoth() {
        super.testCopyObjectChineseBoth();
    }

    @Test
    public void testCopyObjectSelf() throws Exception {
        super.testCopyObjectSelf();
    }

    @Test
    public void testCopyObjectWithMeta() throws Exception {
        super.testCopyObjectWithMeta();
    }

    @Test
    public void testUpdateMetadata() {
        super.testUpdateMetadata();
    }

    @Test
    public void testVerifyRead() {
        super.testVerifyRead();
    }

    @Test
    public void testStreamObjectBetweenBuckets() throws Exception {
        super.testStreamObjectBetweenBuckets();
    }

    @Test
    public void testReadObjectStreamRange() throws Exception {
        super.testReadObjectStreamRange();
    }

    @Test
    public void testGetObjectResultTemplate() {
        super.testGetObjectResultTemplate();
    }

    @Test
    public void testBucketVersions() throws Exception {
        super.testBucketVersions();
    }

    @Test
    public void testDeleteObjectsRequest() {
        super.testDeleteObjectsRequest();
    }

    @Test
    public void testGetObjectMetadata() {
        super.testGetObjectMetadata();
    }

    @Test
    public void testGetObjectVersionMetadata() {
        super.testGetObjectVersionMetadata();
    }

    @Test
    public void testGetObjectMetadataNoExist() {
        super.testGetObjectMetadataNoExist();
    }

    @Test
    public void testGetObjectMetadataRequest() {
        super.testGetObjectMetadataRequest();
    }

    @Test
    public void testGetObjectAcl() {
        super.testGetObjectAcl();
    }

    @Test
    public void testGetObjectVersionAcl() {
        super.testGetObjectVersionAcl();
    }

    @Test
    public void testSetObjectCannedAcl() {
        super.testSetObjectCannedAcl();
    }


    @Test
    public void testSetObjectAclRequestCanned() {
        super.testSetObjectAclRequestCanned();
    }

    @Test
    public void testExtendObjectRetentionPeriod() throws Exception {
        super.testExtendObjectRetentionPeriod();
    }

    @Test
    public void testPreSignedUrl() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl("johnsmith", "photos/puppy.jpg", new Date(1175139620000L));
        Assert.assertEquals("http://10.246.153.111:9020/johnsmith/photos/puppy.jpg" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=DPh574j4iMkdamN4ATyjVQx8Xbk%3D" +
                        "&X-Amz-Security-Token=" + SESSION_TOKEN,
                url.toString());
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
                        "&Signature=qoUcMsSsvGhG8hK078svCbl1HxQ%3D" +
                        "&X-Amz-Security-Token=" + SESSION_TOKEN,
                url.toString());

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

    @Test
    public void testPreSignedPutNoContentType() throws Exception {
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L)));
        Assert.assertEquals("http://10.246.153.111:9020/static.johnsmith.net/db-backup.dat.gz" +
                        "?AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1175139620" +
                        "&Signature=i5GCI%2B1hKjhPe8mLb7Yi2g1MkQE%3D" +
                        "&X-Amz-Security-Token=" + SESSION_TOKEN,
                url.toString());

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
        S3Client tempClient = getPresignDummyClient();
        URL url = tempClient.getPresignedUrl("test-bucket", "解析依頼C1B068.txt", new Date(1500998758000L));
        Assert.assertEquals("http://10.246.153.111:9020/test-bucket/%E8%A7%A3%E6%9E%90%E4%BE%9D%E9%A0%BCC1B068.txt?" +
                        "AWSAccessKeyId=ASIAB51133607AA785B5&Expires=1500998758" +
                        "&Signature=VT%2B3Yl9Vbg8MU%2FIvLrpK1H%2BLfUI%3D" +
                        "&X-Amz-Security-Token=" + SESSION_TOKEN,
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
                        "&Signature=CINGoX001if%2Bz18UBYD6casmBSY%3D" +
                        "&X-Amz-Security-Token=" + SESSION_TOKEN,
                url.toString());
    }

    @Test
    public void testPreSignedUrlHeaderOverrides() throws Exception {
        super.testPreSignedUrlHeaderOverrides();
    }

    @Test
    public void testVPoolHeader() throws Exception {
        super.testVPoolHeader();
    }

    /**
     * A debugging proxy (Fiddler, Charles), is required to verify that the proper header is being sent.
     * Optionally a jersey filter could be used to sniff for it
     */
    @Test
    public void testCustomHeader() {
        super.testCustomHeader();
    }

    @Test
    public void testStaleReadsAllowed() {
        super.testStaleReadsAllowed();
    }

    @Test
    public void testListMarkerWithSpecialChars() {
        super.testListMarkerWithSpecialChars();
    }

    @Test
    public void testListPagesNoDelimiter() {
        super.testListPagesNoDelimiter();
    }

    @Test
    public void testListMarkerWithIllegalChars() {
        super.testListMarkerWithIllegalChars();
    }

    @Test
    public void testPing() {
        super.testPing();
    }

    @Test
    public void testTimeouts() throws Exception {
        super.testTimeouts();
    }

    @Test
    public void testFaultInjection() throws Exception {
        super.testFaultInjection();
    }

    @Test
    public void testCifsEcs() {
        super.testCifsEcs();
    }

    @Test
    public void testListBuckets() throws Exception {
        super.testListBuckets();
    }

    @Ignore
    @Test
    public void testMultipleVdcs() throws Exception {
    }

    @Ignore
    @Test
    public void testCreateExistingBucket() throws Exception {
    }

    @Ignore
    @Test
    public void testMpuAbortInMiddle() throws Exception {
    }

    @Ignore
    @Test
    public void testSetObjectAclRequestAcl() throws Exception {
    }

    @Ignore
    @Test
    public void testSetObjectAcl() throws Exception {
    }

    @Ignore
    @Test
    public void testCreateFilesystemBucket() {
        super.testCreateFilesystemBucket();
    }

    @Ignore
    @Test
    public void testSetBucketAclCanned() throws Exception {
        super.testSetBucketAclCanned();
    }

    @Ignore
    @Test
    public void testSetGetBucketAcl() throws Exception {
        super.testSetGetBucketAcl();
    }

    private S3Client getPresignDummyClient() throws URISyntaxException {
        return new S3JerseyClient(new S3Config(new URI("http://10.246.153.111:9020"))
                .withIdentity("ASIAB51133607AA785B5").withSecretKey("rhkMxcjRq6iaW1KHAdy1QuO9Qi_LCDX9cuk3XUvsgkc")
                .withSessionToken(SESSION_TOKEN));
    }
}
