package com.emc.object;

public interface EntityRequest<T> {
    public T getEntity();

    public String getContentType();
}
