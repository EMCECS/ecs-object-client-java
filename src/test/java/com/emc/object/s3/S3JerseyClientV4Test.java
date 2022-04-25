package com.emc.object.s3;

import com.emc.object.Method;
import com.emc.object.s3.bean.AbstractDeleteResult;
import com.emc.object.s3.bean.DeleteError;
import com.emc.object.s3.bean.DeleteObjectsResult;
import com.emc.object.s3.bean.DeleteSuccess;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import com.sun.jersey.api.client.Client;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class S3JerseyClientV4Test extends S3JerseyClientTest {
    private static final Logger log = LoggerFactory.getLogger(S3JerseyClientV4Test.class);

    @Override
    public S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config().withUseV2Signer(false));
    }

    @Override
    public void testPreSignedUrl() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .withUseV2Signer(false);
        S3Client tempClient = new S3JerseyClient(s3Config);
        URL url = tempClient.getPresignedUrl("johnsmith", "photos/puppy.jpg", new Date(1175139620000L));
        System.out.println("url: " + url);
        assert url.toString().contains("https://johnsmith.s3.amazonaws.com/photos/puppy.jpg?Action=GET&" +
                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F") &
                url.toString().contains("%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=") &
                url.toString().contains("&X-Amz-Expires=") & url.toString().contains("&X-Amz-Signature=") &
                url.toString().contains("&X-Amz-SignedHeaders");
    }

    @Override
    public void testPreSignedPutUrl() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .withUseV2Signer(false);
        S3Client tempClient = new S3JerseyClient(s3Config);

        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L))
                        .withObjectMetadata(new S3ObjectMetadata().withContentType("application/x-download")
                                .withContentMd5("4gJE4saaMU4BqNR0kLY+lw==")
                                .addUserMetadata("checksumalgorithm", "crc32")
                                .addUserMetadata("filechecksum", "0x02661779")
                                .addUserMetadata("reviewedby", "joe@johnsmith.net,jane@johnsmith.net"))
        );

        assert url.toString().contains("https://static.johnsmith.net.s3.amazonaws.com/db-backup.dat.gz?Action=PUT&" +
                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F") &
                url.toString().contains("%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=") &
                url.toString().contains("&X-Amz-Expires=") & url.toString().contains("&X-Amz-Signature=") &
                url.toString().contains("&X-Amz-SignedHeaders=content-md5%3Bcontent-type%3Bx-amz-meta-checksumalgorithm%3Bx-amz-meta-filechecksum%3Bx-amz-meta-reviewedby");

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

    @Override
    public void testPreSignedPutNoContentType() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .withUseV2Signer(false);
        S3Client tempClient = new S3JerseyClient(s3Config);
        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, "static.johnsmith.net", "db-backup.dat.gz", new Date(1175139620000L)));
        assert url.toString().contains("https://static.johnsmith.net.s3.amazonaws.com/db-backup.dat.gz?Action=PUT&" +
                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F") &
                url.toString().contains("%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=") &
                url.toString().contains("&X-Amz-Expires=") & url.toString().contains("&X-Amz-Signature=") &
                url.toString().contains("&X-Amz-SignedHeaders");

        // test real PUT
        // only way is to use HttpURLConnection directly
        String key = "pre-signed-put-test-2";
        url = client.getPresignedUrl(
                new PresignedUrlRequest(Method.PUT, getTestBucket(), key, new Date(System.currentTimeMillis() + 100000))
                        .withObjectMetadata(new S3ObjectMetadata().addUserMetadata("foo", "bar")));

        // uncomment to see the next call in a proxy
        //System.setProperty("http.proxyHost", "127.0.0.1");
        //System.setProperty("http.proxyPort", "8888");
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

    @Override
    public void testPreSignedUrlWithChinese() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("stu").withSecretKey("/QcPo5pEvQh7EOHKs2XjzCARrt7HokZhlpdGKbHs")
                .withUseV2Signer(false);
        S3Client tempClient = new S3JerseyClient(s3Config);
        URL url = tempClient.getPresignedUrl("test-bucket", "解析依頼C1B068.txt", new Date(1500998758000L));
        assert url.toString().contains("https://test-bucket.s3.amazonaws.com/%E8%A7%A3%E6%9E%90%E4%BE%9D%E9%A0%BCC1B068.txt?Action=GET&" +
                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=stu") &
                url.toString().contains("%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=") &
                url.toString().contains("&X-Amz-Expires=") & url.toString().contains("&X-Amz-Signature=") &
                url.toString().contains("&X-Amz-SignedHeaders=");
    }

    @Override
    public void testPreSignedUrlHeaderOverrides() throws Exception {
        S3Config s3Config = new S3Config(new URI("https://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .withUseV2Signer(false);
        S3Client tempClient = new S3JerseyClient(s3Config);

        URL url = tempClient.getPresignedUrl(
                new PresignedUrlRequest(
                        Method.PUT, "johnsmith", "photos/puppy.jpg", new Date(1175139620000L))
                        .withObjectMetadata(
                                new S3ObjectMetadata().withContentType("image/jpeg")
                                        .withContentMd5("4gJE4saaMU4BqNR0kLY+lw==")));
        assert url.toString().contains("johnsmith.s3.amazonaws.com/photos/puppy.jpg?Action=PUT&" +
                "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F") &
                url.toString().contains("%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=") &
                url.toString().contains("&X-Amz-Expires=") & url.toString().contains("&X-Amz-Signature=") &
                url.toString().contains("&X-Amz-SignedHeaders=content-md5%3Bcontent-type");
    }
}