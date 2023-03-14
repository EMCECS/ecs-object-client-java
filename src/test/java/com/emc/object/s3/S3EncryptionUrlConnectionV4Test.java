package com.emc.object.s3;

import com.emc.codec.CodecChain;
import com.emc.object.EncryptionConfig;
import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

public class S3EncryptionUrlConnectionV4Test extends S3EncryptionClientBasicTest {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-url-connection-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        System.setProperty("http.maxConnections", "100");
        S3Config config = createS3Config().withUseV2Signer(false);
        String proxy = config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        if (proxy != null) {
            URI proxyUri = new URI(proxy);
            System.setProperty("http.proxyHost", proxyUri.getHost());
            System.setProperty("http.proxyPort", "" + proxyUri.getPort());
        }
        rclient = new S3JerseyClient(config);
        EncryptionConfig eConfig = createEncryptionConfig();
        eclient = new S3EncryptionClient(config, eConfig);
        encodeSpec = eConfig.getEncryptionSpec();
        if (eConfig.isCompressionEnabled()) encodeSpec = eConfig.getCompressionSpec() + "," + encodeSpec;
        return eclient;
    }

    @Override
    @Test
    public void testRetries() throws Exception {
        byte[] data = "Testing retries!!".getBytes();
        String key = "retry-test";

        S3Config _config = createS3Config().withUseV2Signer(false);
        _config.setFaultInjectionRate(0.4f);
        _config.setRetryLimit(6);
        S3Client _client = new S3EncryptionClient(_config, createEncryptionConfig());

        // make sure we hit at least one error
        for (int i = 0; i < 6; i++) {
            _client.putObject(getTestBucket(), key, data, null);
            S3ObjectMetadata metadata = rclient.getObjectMetadata(getTestBucket(), key);
            Assert.assertEquals(encodeSpec, metadata.getUserMetadata(CodecChain.META_TRANSFORM_MODE));
        }
    }
}
