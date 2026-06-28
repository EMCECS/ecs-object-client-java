package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;

public class WriteTruncationV4Test extends WriteTruncationTest {
    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config().withRetryEnabled(false).withUseV2Signer(false));
    }

    @Override
    protected String getTestBucketPrefix() {
        return "s3-write-truncation-test-v4";
    }
}
