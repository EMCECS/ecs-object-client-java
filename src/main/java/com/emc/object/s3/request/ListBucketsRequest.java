package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.ObjectRequest;
import com.emc.object.s3.S3Constants;

import java.util.Map;

public class ListBucketsRequest extends ObjectRequest {
    private Integer limit;
    private String marker;

    public ListBucketsRequest() {
        super(Method.GET, "", null);
    }

    @Override
    public Map<String, Object> getQueryParams() {
        Map<String, Object> queryMap = super.getQueryParams();
        if (limit != null) queryMap.put(S3Constants.PARAM_LIMIT, limit);
        if (marker != null) queryMap.put(S3Constants.PARAM_MARKER, marker);
        return queryMap;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public ListBucketsRequest withLimit(Integer limit) {
        setLimit(limit);
        return this;
    }

    public ListBucketsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }
}
