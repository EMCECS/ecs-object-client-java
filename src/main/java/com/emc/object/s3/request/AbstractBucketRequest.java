package com.emc.object.s3.request;

import com.emc.object.Method;

public abstract class AbstractBucketRequest extends S3Request {
    private String bucketName;

    public AbstractBucketRequest(Method method, String bucketName, String path) {
        super(method, path);
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
