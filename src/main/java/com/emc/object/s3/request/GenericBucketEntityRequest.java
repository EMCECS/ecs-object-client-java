package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;

public class GenericBucketEntityRequest<T> extends AbstractBucketRequest<GenericBucketEntityRequest>
        implements EntityRequest {
    private Method method;
    private String query;
    private T entity;
    private String contentType;

    public GenericBucketEntityRequest(Method method, String query, T entity, String contentType) {
        this.method = method;
        this.query = query;
        this.entity = entity;
        this.contentType = contentType;
    }

    @Override
    protected GenericBucketEntityRequest me() {
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

    @Override
    public T getEntity() {
        return entity;
    }

    @Override
    public String getContentType() {
        return contentType;
    }
}
