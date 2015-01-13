package com.emc.object.s3.request;

public abstract class AbstractBucketRequest implements S3Request {
    private String bucketName;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public AbstractBucketRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }
}
