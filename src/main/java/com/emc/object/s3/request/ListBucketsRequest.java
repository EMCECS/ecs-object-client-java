package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;

import java.util.HashMap;
import java.util.Map;

public class ListBucketsRequest extends S3Request {
    private Integer limit;
    private String marker;

    public ListBucketsRequest() {
        super(Method.GET, "");
    }

    @Override
    public String getQuery() {
        Map<String, Object> queryMap = new HashMap<>();
        if (limit != null) queryMap.put(S3Constants.PARAM_LIMIT, limit);
        if (marker != null) queryMap.put(S3Constants.PARAM_MARKER, marker);
        return RestUtil.generateQuery(queryMap);
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
