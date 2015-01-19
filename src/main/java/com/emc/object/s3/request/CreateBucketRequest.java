package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CreateBucketRequest extends AbstractBucketRequest<CreateBucketRequest> {
    private CannedAcl cannedAcl;
    private AccessControlList acl;
    private String projectId;
    private String vPoolId;
    private Boolean fileSystemEnabled;

    @Override
    protected CreateBucketRequest me() {
        return this;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();

        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        if (acl != null) headers.putAll(acl.toHeaders());
        if (projectId != null) RestUtil.putSingle(headers, RestUtil.EMC_PROJECT_ID, projectId);
        if (vPoolId != null) RestUtil.putSingle(headers, RestUtil.EMC_VPOOL_ID, vPoolId);
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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

    public CreateBucketRequest withProjectId(String projectId) {
        setProjectId(projectId);
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
