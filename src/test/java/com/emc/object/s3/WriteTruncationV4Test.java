package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.object.s3.request.CreateBucketRequest;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class WriteTruncationV4Test extends WriteTruncationTest{
    public static final Logger log = LoggerFactory.getLogger(WriteTruncationV4Test.class);

    @Override
    @Before
    public void setup() throws Exception {
        S3Config s3Config = AbstractS3ClientTest.s3ConfigFromProperties().withUseV2Signer(false);
        s3Config.setRetryEnabled(false);

        String proxy = s3Config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        if (proxy != null) {
            URI proxyUri = new URI(proxy);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }

        s3Client = new S3JerseyClient(s3Config);
        s3JvmClient = new S3JerseyClient(s3Config, new URLConnectionClientHandler());

        try {
            // create bucket with retention period and D@RE enabled
            s3Client.createBucket(new CreateBucketRequest(BUCKET_NAME)
                    .withRetentionPeriod(OBJECT_RETENTION_PERIOD)
                    .withEncryptionEnabled(true));
        } catch (S3Exception e) {
            if (!e.getErrorCode().equals("BucketAlreadyExists")) throw e;
        }
    }
}
