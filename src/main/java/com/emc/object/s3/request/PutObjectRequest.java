package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;

public class PutObjectRequest<T> extends AbstractObjectRequest implements EntityRequest<T> {
    private T entity;
    private String contentType;

    public PutObjectRequest(String bucketName, String key, T entity) {
        super(Method.PUT, bucketName, key);
        this.entity = entity;
    }

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public T getEntity() {
        return entity;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public PutObjectRequest withContentType(String contentType) {
        setContentType(contentType);
        return this;
    }
}
