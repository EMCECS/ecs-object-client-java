/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.Method;

public class GetObjectMetadataRequest extends GetObjectRequest<GetObjectMetadataRequest> {
    public GetObjectMetadataRequest(String bucketName, String key) {
        super(Method.HEAD, bucketName, key);
    }
}
