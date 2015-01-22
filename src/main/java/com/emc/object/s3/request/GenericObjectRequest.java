package com.emc.object.s3.request;

import com.emc.object.Method;

public class GenericObjectRequest extends AbstractObjectRequest {
    private String query;

    public GenericObjectRequest(Method method, String bucketName, String key) {
        super(method, bucketName, key);
    }

    @Override
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public GenericObjectRequest withQuery(String query) {
        setQuery(query);
        return this;
    }
}
