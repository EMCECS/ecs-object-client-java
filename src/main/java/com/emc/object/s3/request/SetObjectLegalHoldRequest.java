package com.emc.object.s3.request;
import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.bean.ObjectLockLegalHold;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class SetObjectLegalHoldRequest extends S3ObjectRequest implements EntityRequest {
    private ObjectLockLegalHold legalHold;
    private String versionId;

    public SetObjectLegalHoldRequest(String bucketName, String key) {
        super(Method.PUT, bucketName, key, "legal-hold");
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
        return legalHold;
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    public ObjectLockLegalHold getLegalHold() {
        return this.legalHold;
    }

    public void setLegalHold(ObjectLockLegalHold legalHold) {
        this.legalHold = legalHold;
    }

    public SetObjectLegalHoldRequest withLegalHold(ObjectLockLegalHold legalHold) {
        setLegalHold(legalHold);
        return this;
    }

    public String getVersionId() {
        return this.versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public SetObjectLegalHoldRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }
}
