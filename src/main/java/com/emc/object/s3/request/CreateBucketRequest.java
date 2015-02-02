/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CreateBucketRequest extends AbstractBucketRequest {
    private CannedAcl cannedAcl;
    private AccessControlList acl;
    private String vPoolId;
    private Boolean fileSystemEnabled;

    public CreateBucketRequest(String bucketName) {
        super(Method.PUT, bucketName, "", null);
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();

        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        if (acl != null) headers.putAll(acl.toHeaders());
        if (vPoolId != null) RestUtil.putSingle(headers, RestUtil.EMC_VPOOL, vPoolId);
        if (fileSystemEnabled != null) RestUtil.putSingle(headers, RestUtil.EMC_FS_ENABLED, fileSystemEnabled);

        return headers;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public String getvPoolId() {
        return vPoolId;
    }

    public void setvPoolId(String vPoolId) {
        this.vPoolId = vPoolId;
    }

    public Boolean getFileSystemEnabled() {
        return fileSystemEnabled;
    }

    public void setFileSystemEnabled(Boolean fileSystemEnabled) {
        this.fileSystemEnabled = fileSystemEnabled;
    }

    public CreateBucketRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }

    public CreateBucketRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public CreateBucketRequest withVPoolId(String vPoolId) {
        setvPoolId(vPoolId);
        return this;
    }

    public CreateBucketRequest withFileSystemEnabled(Boolean fileSystemEnabled) {
        setFileSystemEnabled(fileSystemEnabled);
        return this;
    }
}
