/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3;

import com.emc.object.Method;
import com.emc.object.s3.request.PresignedUrlRequest;
import com.sun.jersey.core.header.OutBoundHeaders;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class S3AuthUtilTest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    private static final String METHOD_1 = "GET";
    private static final String RESOURCE_1 = "/johnsmith/photos/puppy.jpg";
    private static Map<String, String> PARAMETERS_1 = new HashMap<>();
    private static OutBoundHeaders HEADERS_1 = new OutBoundHeaders();
    private static final String SIGN_STRING_1 = "GET\n" +
            "\n" +
            "\n" +
            "Tue, 27 Mar 2007 19:36:42 +0000\n" +
            "/johnsmith/photos/puppy.jpg";
    private static final String SIGNATURE_1 = "bWq2s1WEIj+Ydj0vQ697zp+IXMU=";

    private static final String METHOD_2 = "PUT";
    private static final String RESOURCE_2 = "/static.johnsmith.net/db-backup.dat.gz";
    private static Map<String, String> PARAMETERS_2 = new HashMap<>();
    private static OutBoundHeaders HEADERS_2 = new OutBoundHeaders();
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
    private static Map<String, String> PARAMETERS_3 = new HashMap<>();
    private static OutBoundHeaders HEADERS_3 = new OutBoundHeaders();
    private static final String SIGN_STRING_3 = "GET\n" +
            "\n" +
            "\n" +
            "Tue, 27 Mar 2007 19:42:41 +0000\n" +
            "/johnsmith/";
    private static final String SIGNATURE_3 = "htDYFYduRNen8P9ZfE/s9SuKy0U=";

    private static final String METHOD_4 = "GET";
    private static final String RESOURCE_4 = "/johnsmith/";
    private static Map<String, String> PARAMETERS_4 = new HashMap<>();
    private static OutBoundHeaders HEADERS_4 = new OutBoundHeaders();
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
    public void testSign() {
        MultivaluedMap<String, Object> headers = new OutBoundHeaders(HEADERS_1);
        S3AuthUtil.sign(METHOD_1, RESOURCE_1, PARAMETERS_1, headers, ACCESS_KEY, SECRET_KEY, 0);
        Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_1, headers.getFirst("Authorization"));

        headers = new OutBoundHeaders(HEADERS_2);
        S3AuthUtil.sign(METHOD_2, RESOURCE_2, PARAMETERS_2, headers, ACCESS_KEY, SECRET_KEY, 0);
        Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_2, headers.getFirst("Authorization"));

        headers = new OutBoundHeaders(HEADERS_3);
        S3AuthUtil.sign(METHOD_3, RESOURCE_3, PARAMETERS_3, headers, ACCESS_KEY, SECRET_KEY, 0);
        Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_3, headers.getFirst("Authorization"));

        headers = new OutBoundHeaders(HEADERS_4);
        S3AuthUtil.sign(METHOD_4, RESOURCE_4, PARAMETERS_4, headers, ACCESS_KEY, SECRET_KEY, 0);
        Assert.assertEquals("AWS " + ACCESS_KEY + ":" + SIGNATURE_4, headers.getFirst("Authorization"));
    }

    @Test
    public void testStringToSign() {
        String stringToSign = S3AuthUtil.getStringToSign(METHOD_1, RESOURCE_1, PARAMETERS_1, HEADERS_1, 0);
        Assert.assertEquals(SIGN_STRING_1, stringToSign);

        stringToSign = S3AuthUtil.getStringToSign(METHOD_2, RESOURCE_2, PARAMETERS_2, HEADERS_2, 0);
        Assert.assertEquals(SIGN_STRING_2, stringToSign);

        stringToSign = S3AuthUtil.getStringToSign(METHOD_3, RESOURCE_3, PARAMETERS_3, HEADERS_3, 0);
        Assert.assertEquals(SIGN_STRING_3, stringToSign);

        stringToSign = S3AuthUtil.getStringToSign(METHOD_4, RESOURCE_4, PARAMETERS_4, HEADERS_4, 0);
        Assert.assertEquals(SIGN_STRING_4, stringToSign);
    }

    @Test
    public void testSignature() {
        Assert.assertEquals(SIGNATURE_1, S3AuthUtil.getSignature(SIGN_STRING_1, SECRET_KEY));

        Assert.assertEquals(SIGNATURE_2, S3AuthUtil.getSignature(SIGN_STRING_2, SECRET_KEY));

        Assert.assertEquals(SIGNATURE_3, S3AuthUtil.getSignature(SIGN_STRING_3, SECRET_KEY));

        Assert.assertEquals(SIGNATURE_4, S3AuthUtil.getSignature(SIGN_STRING_4, SECRET_KEY));
    }

    @Test
    public void testPresignedUrl() throws Exception {
        S3Config s3Config = new S3VHostConfig(new URI("http://s3.amazonaws.com"))
                .withIdentity("AKIAIOSFODNN7EXAMPLE").withSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        PresignedUrlRequest request = new PresignedUrlRequest(Method.GET, "johnsmith", "/photos/puppy.jpg",
                new Date(1175139620));

        String expectedUrl = "http://johnsmith.s3.amazonaws.com/photos/puppy.jpg" +
                "?AWSAccessKeyId=AKIAIOSFODNN7EXAMPLE" +
                "&Expires=1175139620" +
                "&Signature=NpgCjnDzrM%2BWFzoENXmpNDUsSn8%3D";
        String actualUrl = S3AuthUtil.generatePresignedUrl(request, s3Config).toString();

        Assert.assertEquals(expectedUrl, actualUrl);
    }
}
