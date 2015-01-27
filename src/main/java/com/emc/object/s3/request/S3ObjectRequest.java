package com.emc.object.s3.request;

import com.emc.object.Method;

public class S3ObjectRequest extends AbstractBucketRequest {
    private String key;

    public S3ObjectRequest(Method method, String bucketName, String key, String subresource) {
        super(method, bucketName, key, subresource);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
