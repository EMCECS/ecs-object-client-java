/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.ObjectRequest;

public abstract class AbstractBucketRequest extends ObjectRequest {
    private String bucketName;

    public AbstractBucketRequest(Method method, String bucketName, String path, String subresource) {
        super(method, path, subresource);
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
