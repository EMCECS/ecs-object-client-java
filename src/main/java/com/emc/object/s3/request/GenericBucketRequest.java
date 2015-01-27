package com.emc.object.s3.request;

import com.emc.object.Method;

public class GenericBucketRequest extends AbstractBucketRequest {
    public GenericBucketRequest(Method method, String bucketName, String subresource) {
        super(method, bucketName, "", subresource);
    }
}
