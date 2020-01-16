package com.emc.object.s3;

import com.sun.jersey.core.header.OutBoundHeaders;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class S3V4AuthUtilTest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final String V4_DATE = "20070327";

    private static Map<String, String> PARAMETERS_1 = new HashMap<String, String>();
    private static OutBoundHeaders HEADERS_1 = new OutBoundHeaders();

    @BeforeClass
    public static void setup() {
        HEADERS_1.putSingle("Host", "johnsmith.s3.amazonaws.com");
        HEADERS_1.putSingle("Date", "Tue, 27 Mar 2007 19:36:42 +0000");

    }

    @Test
    public void testGetDate() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);
        Assert.assertEquals(V4_DATE, signer.getDate(PARAMETERS_1, HEADERS_1));
        System.out.println(signer.getDate(PARAMETERS_1, HEADERS_1));
    }
}