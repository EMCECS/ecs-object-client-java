package com.emc.object.s3;

import com.emc.object.ObjectRequest;
import com.emc.object.s3.request.PutObjectRequest;
import com.sun.jersey.api.client.ClientRequest;
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
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "s3";
    private static final String EXPECTED_SCOPE = V4_DATE + "/" + REGION + "/" + SERVICE + "/" + S3Constants.AWS_V4_TERMINATOR;

    private static Map<String, String> PARAMETERS_1 = new HashMap<String, String>();
    private static OutBoundHeaders HEADERS_1 = new OutBoundHeaders();

    private static String payload = "{\n" +
            "\"service_id\": \"09cac1c6-1b0a-11e6-b6ba-3e1d05defe78\",\n" +
            "\"plan_id\": \"09cac5b8-1b0a-11e6-b6ba-3e1d05defe78\",\n" +
            "\"organization_guid\": \"sup\",\n" +
            "\"space_guid\": \"dawg\",\n" +
            "\"context\": {},\n" +
            "\"parameters\": {}\n" +
            "}";

    private static PutObjectRequest request = new PutObjectRequest("testBucket", "testKey", payload);

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

    @Test
    public void testGetScope() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);
        Assert.assertEquals(EXPECTED_SCOPE, signer.getScope(PARAMETERS_1, HEADERS_1));
    }

    @Test
    public void testPayloadHash() throws Exception {

    }
}