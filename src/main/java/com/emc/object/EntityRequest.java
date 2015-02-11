/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object;

public interface EntityRequest<T> {
    public T getEntity();

    public String getContentType();

    public Long getContentLength();
}
