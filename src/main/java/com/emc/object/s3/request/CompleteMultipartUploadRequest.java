/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.bean.CompleteMultipartUpload;
import com.emc.object.s3.bean.MultipartPartETag;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CompleteMultipartUploadRequest extends S3ObjectRequest implements EntityRequest<CompleteMultipartUpload> {
    private String uploadId;
    private List<MultipartPartETag> parts;

    public CompleteMultipartUploadRequest(String bucketName, String key, String uploadId) {
        super(Method.POST, bucketName, key, null);
        this.uploadId = uploadId;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        queryParams.put("uploadId", uploadId);
        return queryParams;
    }

    @Override
    public CompleteMultipartUpload getEntity() {
        return new CompleteMultipartUpload(parts);
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    @Override
    public Long getContentLength() {
        return null; // assuming the XML will be smaller than the configured entity buffer
    }

    public String getUploadId() {
        return uploadId;
    }

    public List<MultipartPartETag> getParts() {
        return parts;
    }

    public void setParts(List<MultipartPartETag> parts) {
        this.parts = parts;
    }

    public CompleteMultipartUploadRequest withParts(List<MultipartPartETag> parts) {
        setParts(parts);
        return this;
    }
}
