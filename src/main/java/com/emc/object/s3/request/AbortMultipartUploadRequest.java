/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;

import java.util.Map;

public class AbortMultipartUploadRequest extends S3ObjectRequest {
    private String uploadId;

    public AbortMultipartUploadRequest(String bucketName, String key, String uploadId) {
        super(Method.DELETE, bucketName, key, null);
        this.uploadId = uploadId;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        queryParams.put(S3Constants.PARAM_UPLOAD_ID, uploadId);
        return queryParams;
    }

    public String getUploadId() {
        return uploadId;
    }
}
