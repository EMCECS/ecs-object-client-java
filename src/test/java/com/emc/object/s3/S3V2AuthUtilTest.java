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
import com.emc.object.s3.request.PresignedUrlRequest;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class S3V2AuthUtilTest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    private static final String METHOD_1 = "GET";
    private static final String RESOURCE_1 = "/johnsmith/photos/puppy.jpg";
    private static Map<String, String> PARAMETERS_1 = new HashMap<String, String>();
    private static MultivaluedMap<String, Object> HEADERS_1 = new MultivaluedHashMap<>();
    private static final String SIGN_STRING_1 = "GET\n" +
            "\n" +
            "\n" +
            "Tue, 27 Mar 2007 19:36:42 +0000\n" +
            "/johnsmith/photos/puppy.jpg";
    private static final String SIGNATURE_1 = "bWq2s1WEIj+Ydj0vQ697zp+IXMU=";

    private static final String METHOD_2 = "PUT";
    private static final String RESOURCE_2 = "/static.johnsmith.net/db-backup.dat.gz";
    private static Map<String, String> PARAMETERS_2 = new HashMap<String, String>();
    private static MultivaluedMap<String, Object> HEADERS_2 = new MultivaluedHashMap<String, Object>();
    private static final String SIGN_STRING_2 = "PUT\n" +
            "4gJE4saaMU4BqNR0kLY+lw==\n" +
            "application/x-download\n" +
            "Tue, 27 Mar 2007 21:06:08 +0000\n" +
            "x-amz-acl:public-read\n" +
            "x-amz-meta-checksumalgorithm:crc32\n" +
            "x-amz-meta-filechecksum:0x02661779\n" +
            "x-amz-meta-reviewedby:joe@johnsmith.net,jane@johnsmith.net\n" +
            "/static.johnsmith.net/db-backup.dat.gz";
    private static final String SIGNATURE_2 = "ilyl83RwaSoYIEdixDQcA4OnAnc=";

    private static final String METHOD_3 = "GET";
    private static final String RESOURCE_3 = "/johnsmith/";
    private static Map<String, String> PARAMETERS_3 = new HashMap<String, String>();
    private static MultivaluedMap<String, Object> HEADERS_3 = new MultivaluedHashMap<String, Object>();
    private static final String SIGN_STRING_3 = "GET\n" +
            "\n" +
            "\n" +
            "Tue, 27 Mar 2007 19:42:41 +0000\n" +
            "/johnsmith/";
    private static final String SIGNATURE_3 = "htDYFYduRNen8P9ZfE/s9SuKy0U=";

    private static final String METHOD_4 = "GET";
    private static final String RESOURCE_4 = "/johnsmith/";
    private static Map<String, String> PARAMETERS_4 = new HashMap<String, String>();
    private static MultivaluedMap<String, Object> HEADERS_4 = new MultivaluedHashMap<>();
    private static final String SIGN_STRING_4 = "GET\n" +
            "\n" +
            "\n" +
            "Tue, 27 Mar 2007 19:44:46 +0000\n" +
            "/johnsmith/?acl";
    private static final String SIGNATURE_4 = "c2WLPFtWHVgbEmeEG93a4cG37dM=";

    @BeforeClass
    public static void setup() {
        HEADERS_1.putSingle("Host", "johnsmith.s3.amazonaws.com");
        HEADERS_1.putSingle("Date", "Tue, 27 Mar 2007 19:36:42 +0000");

        HEADERS_2.putSingle("Content-MD5", "4gJE4saaMU4BqNR0kLY+lw==");
        HEADERS_2.putSingle("Content-Type", "application/x-download");
        HEADERS_2.putSingle("Date", "Tue, 27 Mar 2007 21:06:08 +0000");
        HEADERS_2.putSingle("x-amz-acl", "public-read");
        HEADERS_2.putSingle("x-amz-meta-checksumalgorithm", "crc32");
        HEADERS_2.putSingle("x-amz-meta-filechecksum", "0x02661779");
        HEADERS_2.putSingle("x-amz-meta-reviewedby", "joe@johnsmith.net,jane@johnsmith.net");

        PARAMETERS_3.put("prefix", "photos");
        PARAMETERS_3.put("max-keys", "50");
        PARAMETERS_3.put("marker", "puppy");
        HEADERS_3.putSingle("User-Agent", "Mozilla/5.0");
        HEADERS_3.putSingle("Host", "johnsmith.s3.amazonaws.com");
        HEADERS_3.putSingle("Date", "Tue, 27 Mar 2007 19:42:41 +0000");

        PARAMETERS_4.put("acl", null);
        HEADERS_4.putSingle("Host", "johnsmith.s3.amazonaws.com");
        HEADERS_4.putSingle("Date", "Tue, 27 Mar 2007 19:44:46 +0000");
    }

    @Test
    public void testSign() throws Exception {
        String uri = "http://s3.company.com";

        S3Config s3Config = new S3Config(new URI("http://here.com")).withIdentity(ACCESS_KEY).withSecretKey(SECRET_KEY);
        S3SignerV2 signer = new S3SignerV2(s3Config);

        // In Jersey 2.x, use ClientRequestFilter to sign the request
        JerseyClient client1 = JerseyClientBuilder.createClient();
        client1.register((ClientRequestFilter) requestContext -> {
            requestContext.setUri(URI.create(uri));
            requestContext.setMethod(METHOD_1);
            requestContext.getHeaders().putAll(HEADERS_1);
            signer.sign(requestContext, RESOURCE_1, PARAMETERS_1, HEADERS_1);
            Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_1, HEADERS_1.getFirst("Authorization"));
        });
        try {
            client1.target(uri).request().method(METHOD_1);
        } catch (RuntimeException ignored) {}
        client1.close();

        JerseyClient client2 = JerseyClientBuilder.createClient();
        client2.register((ClientRequestFilter) requestContext -> {
            requestContext.setUri(URI.create(uri));
            requestContext.setMethod(METHOD_2);
            requestContext.getHeaders().putAll(HEADERS_2);
            signer.sign(requestContext, RESOURCE_2, PARAMETERS_2, HEADERS_2);
            Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_2, HEADERS_2.getFirst("Authorization"));
        });
        try {
            client2.target(uri).request().method(METHOD_2);
        } catch (RuntimeException ignored) {}
        client2.close();

        JerseyClient client3 = JerseyClientBuilder.createClient();
        client3.register((ClientRequestFilter) requestContext -> {
            requestContext.setUri(URI.create(uri));
            requestContext.setMethod(METHOD_3);
            requestContext.getHeaders().putAll(HEADERS_3);
            signer.sign(requestContext, RESOURCE_3, PARAMETERS_3, HEADERS_3);
            Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_3, HEADERS_3.getFirst("Authorization"));
        });
        try {
            client3.target(uri).request().method(METHOD_3);
        } catch (RuntimeException ignored) {}
        client3.close();

        JerseyClient client4 = JerseyClientBuilder.createClient();
        client4.register((ClientRequestFilter) requestContext -> {
            requestContext.setUri(URI.create(uri));
            requestContext.setMethod(METHOD_4);
            requestContext.getHeaders().putAll(HEADERS_4);
            signer.sign(requestContext, RESOURCE_4, PARAMETERS_4, HEADERS_4);
            Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_4, HEADERS_4.getFirst("Authorization"));
        });
        try {
            client4.target(uri).request().method(METHOD_4);
        } catch (RuntimeException ignored) {}
        client4.close();
    }

    @Test
    public void testStringToSign() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com")).withIdentity(ACCESS_KEY).withSecretKey(SECRET_KEY);
        S3SignerV2 signer = new S3SignerV2(s3Config);

        String stringToSign = signer.getStringToSign(METHOD_1, RESOURCE_1, PARAMETERS_1, HEADERS_1);
        Assert.assertEquals(SIGN_STRING_1, stringToSign);

        stringToSign = signer.getStringToSign(METHOD_2, RESOURCE_2, PARAMETERS_2, HEADERS_2);
        Assert.assertEquals(SIGN_STRING_2, stringToSign);

        stringToSign = signer.getStringToSign(METHOD_3, RESOURCE_3, PARAMETERS_3, HEADERS_3);
        Assert.assertEquals(SIGN_STRING_3, stringToSign);

        stringToSign = signer.getStringToSign(METHOD_4, RESOURCE_4, PARAMETERS_4, HEADERS_4);
        Assert.assertEquals(SIGN_STRING_4, stringToSign);
    }

    @Test
    public void testSignature() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com")).withIdentity(ACCESS_KEY).withSecretKey(SECRET_KEY);
        S3SignerV2 signer = new S3SignerV2(s3Config);

        Assert.assertEquals(SIGNATURE_1, signer.getSignature(SIGN_STRING_1, null));

        Assert.assertEquals(SIGNATURE_2, signer.getSignature(SIGN_STRING_2, null));

        Assert.assertEquals(SIGNATURE_3, signer.getSignature(SIGN_STRING_3, null));

        Assert.assertEquals(SIGNATURE_4, signer.getSignature(SIGN_STRING_4, null));
    }

    @Test
    public void testPresignedUrl() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://s3.amazonaws.com")).withUseVHost(true)
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        S3SignerV2 signer = new S3SignerV2(s3Config);

        PresignedUrlRequest request = new PresignedUrlRequest(Method.GET, "johnsmith", "photos/puppy.jpg",
                new Date(1175139620000L));

        String expectedUrl = "http://johnsmith.s3.amazonaws.com/photos/puppy.jpg" +
                "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE" +
                "&Expires=1175139620" +
                "&Signature=NpgCjnDzrM%2BWFzoENXmpNDUsSn8%3D";
        String actualUrl = signer.generatePresignedUrl(request).toString();

        Assert.assertEquals(expectedUrl, actualUrl);
    }
}
