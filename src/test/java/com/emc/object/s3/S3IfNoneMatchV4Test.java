package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;

public class S3IfNoneMatchV4Test extends S3IfNoneMatchTest {
    @Override
    protected String getTestBucketPrefix() {
        return "if-none-match-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config().withUseV2Signer(false));
    }
}
