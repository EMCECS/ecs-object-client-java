package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;

public class LargeFileDownloaderV4Test extends LargeFileDownloaderTest {
    @Override
    protected String getTestBucketPrefix() {
        return "lfd-v4-test";
    }

    @Override
    protected S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config().withUseV2Signer(false));
    }
}
