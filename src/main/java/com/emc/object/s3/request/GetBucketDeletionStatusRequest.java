package com.emc.object.s3.request;

import com.emc.object.Method;

public class GetBucketDeletionStatusRequest extends AbstractBucketRequest {
    public GetBucketDeletionStatusRequest(String bucketName) {
        super(Method.GET, bucketName, "", "empty-bucket-status");
    }
}
