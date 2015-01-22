package com.emc.object.s3.request;

import com.emc.object.Method;

public abstract class AbstractObjectRequest extends AbstractBucketRequest {
    private String key;

    public AbstractObjectRequest(Method method, String bucketName, String key) {
        super(method, bucketName, key);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
