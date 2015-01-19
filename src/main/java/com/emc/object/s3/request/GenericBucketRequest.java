package com.emc.object.s3.request;

import com.emc.object.Method;

public class GenericBucketRequest extends AbstractBucketRequest<GenericBucketRequest> {
    private Method method;
    private String query;

    public GenericBucketRequest(Method method, String query) {
        this.method = method;
        this.query = query;
    }

    @Override
    protected GenericBucketRequest me() {
        return this;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public String getQuery() {
        return query;
    }
}
