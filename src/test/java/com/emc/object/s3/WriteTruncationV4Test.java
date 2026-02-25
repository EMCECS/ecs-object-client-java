package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;
// URLConnectionClientHandler removed in Jersey 2.x migration

public class WriteTruncationV4Test extends WriteTruncationTest {
    @Override
    protected S3Client createS3Client() throws Exception {
        S3Config s3Config = createS3Config().withRetryEnabled(false).withUseV2Signer(false);
        this.jvmClient = new S3JerseyClient(s3Config);
        return new S3JerseyClient(createS3Config().withRetryEnabled(false));
    }

    @Override
    protected String getTestBucketPrefix() {
        return "s3-write-truncation-test-v4";
    }
}
