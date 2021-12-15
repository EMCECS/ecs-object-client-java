package com.emc.object.s3;

import com.emc.object.s3.jersey.S3JerseyClient;

/**
 * Tests related to bucket metadata search.
 */
public class S3MetadataSearchV4Test extends S3MetadataSearchTest {
    @Override
    protected String getTestBucketPrefix() {
        return "s3-metadata-search-v4-test";
    }

    @Override
    public S3Client createS3Client() throws Exception {
        return new S3JerseyClient(createS3Config().withUseV2Signer(false));
    }
}
