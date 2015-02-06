package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PresignedUrlRequest extends S3ObjectRequest {
    private Date expirationTime;
    private String versionId;
    private Map<ResponseHeaderOverride, String> headerOverrides = new HashMap<ResponseHeaderOverride, String>();

    public PresignedUrlRequest(Method method, String bucketName, String key, Date expirationTime) {
        super(method, bucketName, key, null);
        this.expirationTime = expirationTime;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put(S3Constants.PARAM_VERSION_ID, versionId);
        queryParams.put(S3Constants.PARAM_EXPIRES, Long.toString(expirationTime.getTime()));
        for (ResponseHeaderOverride override : headerOverrides.keySet()) {
            queryParams.put(override.getQueryParam(), headerOverrides.get(override));
        }
        return queryParams;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Map<ResponseHeaderOverride, String> getHeaderOverrides() {
        return headerOverrides;
    }

    public void setHeaderOverrides(Map<ResponseHeaderOverride, String> headerOverrides) {
        this.headerOverrides = headerOverrides;
    }

    public PresignedUrlRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

    public PresignedUrlRequest headerOverride(ResponseHeaderOverride override, String value) {
        headerOverrides.put(override, value);
        return this;
    }
}
