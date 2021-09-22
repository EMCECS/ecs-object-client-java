package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DeleteObjectRequest extends S3ObjectRequest {
    private String versionId;
    private Date ifUnmodifiedSince;
    private String ifMatch;
    private Boolean bypassGovernanceRetention;

    public DeleteObjectRequest(String bucketName, String key) {
        super(Method.DELETE, bucketName, key, null);
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
        if (ifUnmodifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_UNMODIFIED_SINCE, RestUtil.headerFormat(ifUnmodifiedSince));
        if (ifMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_MATCH, ifMatch);
        if (bypassGovernanceRetention != null) RestUtil.putSingle(headers, S3Constants.AMZ_OBJECT_LOCK_BYPASS_GOVERNANCE_RETENTION, bypassGovernanceRetention.toString());
        return headers;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Date getIfUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
        this.ifUnmodifiedSince = ifUnmodifiedSince;
    }

    public String getIfMatch() {
        return ifMatch;
    }

    public void setIfMatch(String ifMatch) {
        this.ifMatch = ifMatch;
    }

    public boolean getBypassGovernanceRetention() {
        return bypassGovernanceRetention;
    }

    public void  setBypassGovernanceRetention(Boolean bypassGovernanceRetention) {
        this.bypassGovernanceRetention = bypassGovernanceRetention;
    }

    public DeleteObjectRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

    public DeleteObjectRequest withIfUnmodifiedSince(Date ifUnmodifiedSince) {
        setIfUnmodifiedSince(ifUnmodifiedSince);
        return this;
    }

    public DeleteObjectRequest withIfMatch(String ifMatch) {
        setIfMatch(ifMatch);
        return this;
    }

    public DeleteObjectRequest withBypassGovernanceRetention(Boolean bypassGovernanceRetention) {
        setBypassGovernanceRetention(bypassGovernanceRetention);
        return this;
    }
}
