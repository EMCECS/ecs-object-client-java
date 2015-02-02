/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */

package com.emc.object.s3.bean;

import com.emc.object.ObjectResponse;
import com.emc.object.s3.S3ObjectMetadata;

public class GetObjectResult<T> extends ObjectResponse {
    private T object;

    public S3ObjectMetadata getObjectMetadata() {
        return S3ObjectMetadata.fromHeaders(getHeaders());
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
