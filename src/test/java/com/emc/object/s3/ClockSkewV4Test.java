package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;

public class ClockSkewV4Test extends ClockSkewTest {
    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config().withUseV2Signer(false));
    }
}
