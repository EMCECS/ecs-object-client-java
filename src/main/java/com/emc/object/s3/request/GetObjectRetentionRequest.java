package com.emc.object.s3.request;

import com.emc.object.Method;

import java.util.Map;

public class GetObjectRetentionRequest extends S3ObjectRequest {
    private String versionId;

    public GetObjectRetentionRequest(String bucketName, String key) {
        super(Method.GET, bucketName, key, "retention");
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put("versionId", versionId);
        return queryParams;
    }

    public String getVersionId() {
        return this.versionId;
    }

    public GetObjectRetentionRequest withVersionId(String versionId) {
        this.versionId = versionId;
        return this;
    }

    public void setVersionId(String versionId) {
        this.withVersionId(versionId);
    }
}
