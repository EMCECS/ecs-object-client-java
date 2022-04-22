package com.emc.object.s3.request;

import com.emc.object.Method;

import java.util.Map;

public class GetObjectTaggingRequest extends S3ObjectRequest {
    private String versionId;

    public GetObjectTaggingRequest(String bucketName, String key) {
        super(Method.GET, bucketName, key, "tagging");
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

    public GetObjectTaggingRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}
