package com.emc.object.s3;

import com.emc.object.s3.request.PutObjectRequest;
import com.emc.object.util.RestUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3V4AuthUtilTest {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final String V4_DATE = "20150830";
    private static final String AMZ_V4_DATE = "20150830T123600Z";
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "iam";
    private static final String EXPECTED_SCOPE = V4_DATE +
            "/" + REGION + "/" + SERVICE + "/" + S3Constants.AWS_V4_TERMINATOR;
    private static final String EXPECTED_STRING_TO_SIGN = "AWS4-HMAC-SHA256\n" +
            "20150830T123600Z\n" +
            "20150830/us-east-1/iam/aws4_request\n" +
            "f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59";
    private static final String EXPECTED_CANONICAL_REQUEST = "GET\n" +
            "/\n" +
            "Action=ListUsers&Version=2010-05-08\n" +
            "content-type:application/x-www-form-urlencoded; charset=utf-8\n" +
            "host:iam.amazonaws.com\n" +
            "x-amz-date:20150830T123600Z\n" +
            "\n" +
            "content-type;host;x-amz-date\n" +
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final String CANONICAL_REQUEST = "GET\n" +
            "/\n" +
            "Action=ListUsers&Version=2010-05-08\n" +
            "x-amz-date:20150830T123600Z\n" +
            "\n" +
            "x-amz-date\n" +
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String EXPECTED_HASHED_REQUEST =
            "f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59";
    private static final String EXPECTED_SIGNING_KEY =
            "c4afb1cc5771d871763a393e44b703571b55cc28424d1a5e86da6ed3c154a4b9";
    private static final String EXPECTED_SIGNATURE =
            "5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7";
    private static Map<String, String> PARAMETERS_1 = new HashMap<String, String>();
    private static MultivaluedMap<String, Object> HEADERS_1 = new MultivaluedHashMap<String, Object>();

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
        HEADERS_1.putSingle("Date", "Sun, 30 Aug 2015 12:36:00 GMT");

    }

    @Test
    public void testGetDate() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);
        Assert.assertEquals(V4_DATE, signer.getShortDate(signer.getDate(PARAMETERS_1, HEADERS_1)));
    }

    @Test
    public void testGetScope() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);
        Assert.assertEquals(EXPECTED_SCOPE, signer.getScope(V4_DATE, SERVICE));
    }

    @Test
    public void testGetCanonicalRequest() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);

        JerseyClient client = JerseyClientBuilder.createClient();
        client.register((ClientRequestFilter) requestContext -> {
            requestContext.setUri(URI.create("https://iam.amazonaws.com/?Action=ListUsers&Version=2010-05-08"));
            requestContext.setMethod("GET");
            Map<String, String> parameters = RestUtil.getQueryParameterMap(requestContext.getUri().getRawQuery());
            Map<String, List<Object>> headers = new HashMap<>();
            RestUtil.putSingle(headers,S3Constants.AMZ_DATE, AMZ_V4_DATE);
            Assert.assertEquals(CANONICAL_REQUEST, signer.getCanonicalRequest(requestContext.getMethod(), requestContext.getUri(), parameters, headers, false));
        });
        client.target("https://iam.amazonaws.com/?Action=ListUsers&Version=2010-05-08").request().get();
        client.close();
    }

    @Test
    public void testGetStringToSign() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);
        S3SignerV4 signer = new S3SignerV4(s3Config);
        Assert.assertEquals(EXPECTED_STRING_TO_SIGN,
                signer.getStringToSign(null, null, null, null, AMZ_V4_DATE, SERVICE, EXPECTED_CANONICAL_REQUEST));
    }

    @Test
    public void testGetSigningKey() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);
        S3SignerV4 signer = new S3SignerV4(s3Config);
        byte[] signingKey = signer.getSigningKey(V4_DATE, S3Constants.AWS_SERVICE_IAM);
        Assert.assertEquals(EXPECTED_SIGNING_KEY, signer.hexEncode(signingKey));
    }

    @Test
    public void testGetSignature() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);
        String stringToSign = EXPECTED_STRING_TO_SIGN;
        byte[] signingKey = signer.getSigningKey(V4_DATE, S3Constants.AWS_SERVICE_IAM);
        Assert.assertEquals(EXPECTED_SIGNATURE, signer.getSignature(stringToSign, signingKey));
    }

    @Test
    public void testGetShortDate() throws Exception {
        S3Config s3Config = new S3Config(new URI("http://here.com"))
                .withIdentity(ACCESS_KEY)
                .withSecretKey(SECRET_KEY);

        S3SignerV4 signer = new S3SignerV4(s3Config);
        Assert.assertEquals(V4_DATE, signer.getShortDate(AMZ_V4_DATE));
    }
}