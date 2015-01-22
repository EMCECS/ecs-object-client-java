package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.ObjectRequest;

public abstract class S3Request extends ObjectRequest {
    public S3Request(Method method, String path) {
        super(method, path);
    }
}
