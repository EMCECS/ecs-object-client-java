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
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.*;
import com.sun.jersey.api.client.Client;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class S3JerseyClientV4Test extends S3JerseyClientTest {
    private static final Logger l4j = Logger.getLogger(S3JerseyClientV4Test.class);
    private boolean testIAM = false;

    @Override
    public S3Client createS3Client() throws Exception {
        testIAM = isIAMUser();
        return new S3JerseyClient(createS3Config().withUseV2Signer(false));
    }

    @Ignore
    @Test
    public void testPreSignedUrl() throws Exception {
    }

    @Test
    public void testPreSignedUrlV4() throws Exception {
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

    @Ignore
    @Test
    public void testPreSignedPutUrl() throws Exception {
    }

    @Test
    public void testPreSignedPutUrlV4() throws Exception {
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

    @Ignore
    @Test
    public void testPreSignedPutNoContentType() throws Exception {
    }

    @Test
    public void testPreSignedPutNoContentTypeV4() throws Exception {
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

    @Ignore
    @Test
    public void testPreSignedUrlWithChinese() throws Exception {
    }

    @Test
    public void testPreSignedUrlWithChineseV4() throws Exception {
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

    @Test
    public void testPreSignedUrlWithHeadersV4() throws Exception {
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

    @Ignore
    @Test
    public void testPreSignedUrlHeaderOverrides() throws Exception {
    }
}