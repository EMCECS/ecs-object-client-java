/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class InitiateMultipartUploadRequest extends S3ObjectRequest {
    private S3ObjectMetadata objectMetadata;
    private AccessControlList acl;
    private CannedAcl cannedAcl;

    public InitiateMultipartUploadRequest(String bucketName, String key) {
        super(Method.POST, bucketName, key, "uploads");
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (objectMetadata != null) headers.putAll(objectMetadata.toHeaders());
        if (acl != null) headers.putAll(acl.toHeaders());
        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        return headers;
    }

    public S3ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(S3ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public InitiateMultipartUploadRequest withObjectMetadata(S3ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public InitiateMultipartUploadRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public InitiateMultipartUploadRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }
}
