package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.Range;
import com.emc.object.util.RestUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetObjectRequest<T extends GetObjectRequest<T>> extends S3ObjectRequest {
    private String versionId;
    private Range range;
    private Date ifModifiedSince;
    private Date ifUnmodifiedSince;
    private String ifMatch;
    private String ifNoneMatch;
    private Map<ResponseHeaderOverride, String> headerOverrides = new HashMap<>();

    public GetObjectRequest(String bucketName, String key) {
        this(Method.GET, bucketName, key);
    }

    protected GetObjectRequest(Method method, String bucketName, String key) {
        super(method, bucketName, key, null);
    }

    @Override
    public Map<String, Object> getQueryParams() {
        Map<String, Object> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put("versionId", versionId);
        for (ResponseHeaderOverride override : headerOverrides.keySet()) {
            queryParams.put(override.getQueryParam(), headerOverrides.get(override));
        }
        return queryParams;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (range != null) RestUtil.putSingle(headers, RestUtil.HEADER_RANGE, "bytes=" + range.toString());
        if (ifModifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_MODIFIED_SINCE, RestUtil.headerFormat(ifModifiedSince));
        if (ifUnmodifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_UNMODIFIED_SINE, RestUtil.headerFormat(ifUnmodifiedSince));
        if (ifMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_MATCH, ifMatch);
        if (ifNoneMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_NONE_MATCH, ifNoneMatch);
        return headers;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Date getIfModifiedSince() {
        return ifModifiedSince;
    }

    public void setIfModifiedSince(Date ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
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

    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    public void setIfNoneMatch(String ifNoneMatch) {
        this.ifNoneMatch = ifNoneMatch;
    }

    public Map<ResponseHeaderOverride, String> getHeaderOverrides() {
        return headerOverrides;
    }

    public void setHeaderOverrides(Map<ResponseHeaderOverride, String> headerOverrides) {
        this.headerOverrides = headerOverrides;
    }

    @SuppressWarnings("unchecked")
    public T withVersionId(String versionId) {
        setVersionId(versionId);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withRange(Range range) {
        setRange(range);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T headerOverride(ResponseHeaderOverride override, String value) {
        headerOverrides.put(override, value);
        return (T) this;
    }
}
