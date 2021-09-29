package com.emc.object.s3;

import com.emc.codec.CodecChain;
import com.emc.object.EncryptionConfig;
import com.emc.object.s3.jersey.S3EncryptionClient;
import com.emc.object.s3.jersey.S3JerseyClient;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3EncryptionClientBasicV4Test extends S3EncryptionClientBasicTest {
    private static final Logger log = LoggerFactory.getLogger(S3EncryptionClientBasicV4Test.class);

    @Override
    protected String getTestBucketPrefix() {
        return "s3-encryption-client-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        rclient = new S3JerseyClient(createS3Config().withUseV2Signer(false));
        EncryptionConfig eConfig = createEncryptionConfig();
        eclient = new S3EncryptionClient(createS3Config().withUseV2Signer(false), eConfig);
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
