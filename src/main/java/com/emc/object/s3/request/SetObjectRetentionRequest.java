package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.ObjectLockRetention;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class SetObjectRetentionRequest extends S3ObjectRequest implements EntityRequest {
    private ObjectLockRetention retention;
    private Boolean bypassGovernanceRetention;
    private String versionId;

    public SetObjectRetentionRequest(String bucketName, String key) {
        super(Method.PUT, bucketName, key, "retention");
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put("versionId", versionId);
        return queryParams;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (bypassGovernanceRetention != null) RestUtil.putSingle(headers, S3Constants.AMZ_OBJECT_LOCK_BYPASS_GOVERNANCE_RETENTION, bypassGovernanceRetention.toString());
        return headers;
    }

    @Override
    public Long getContentLength() {
        return null; // assume buffering
    }

    @Override
    public boolean isChunkable() {
        return false;
    }

    @Override
    public Object getEntity() {
        return retention;
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    public ObjectLockRetention getRetention() {
        return retention;
    }

    public SetObjectRetentionRequest withRetention(ObjectLockRetention retention) {
        this.retention = retention;
        return this;
    }

    public void setRetention(ObjectLockRetention retention) {
        withRetention(retention);
    }

    public String getVersionId() {
        return versionId;
    }

    public SetObjectRetentionRequest withVersionId(String versionId) {
        this.versionId = versionId;
        return this;
    }

    public void setVersionId(String versionId) {
        withVersionId(versionId);
    }

    public boolean getBypassGovernanceRetention() {
        return bypassGovernanceRetention;
    }

    public SetObjectRetentionRequest withBypassGovernanceRetention(Boolean bypassGovernanceRetention) {
        this.bypassGovernanceRetention = bypassGovernanceRetention;
        return this;
    }

    public void setBypassGovernanceRetention(Boolean bypassGovernanceRetention) {
        withBypassGovernanceRetention(bypassGovernanceRetention);
    }
}
