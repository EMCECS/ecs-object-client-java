package com.emc.object.s3.request;

public abstract class AbstractBucketRequest<T extends AbstractBucketRequest> extends S3Request {
    private String bucketName;

    protected abstract T me();

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public T withBucketName(String bucketName) {
        setBucketName(bucketName);
        return me();
    }
}
