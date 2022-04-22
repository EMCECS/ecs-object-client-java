package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.bean.ObjectTagging;
import com.emc.object.util.RestUtil;

import java.util.Map;

public class PutObjectTaggingRequest extends S3ObjectRequest implements EntityRequest {

    private ObjectTagging tagging;
    private String versionId;

    public PutObjectTaggingRequest(String bucketName, String key) {
        super(Method.PUT, bucketName, key, "tagging");
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put("versionId", versionId);
        return queryParams;
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
        return tagging;
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    public ObjectTagging getTagging() {
        return tagging;
    }

    public void setTagging(ObjectTagging tagging) { this.tagging = tagging; }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public PutObjectTaggingRequest withTagging(ObjectTagging tagging) {
        setTagging(tagging);
        return this;
    }

    public PutObjectTaggingRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

}
