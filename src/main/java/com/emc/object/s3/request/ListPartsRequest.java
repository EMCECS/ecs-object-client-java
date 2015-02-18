package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;

import java.util.Map;

public class ListPartsRequest extends S3ObjectRequest {
    private String uploadId;
    private Integer maxParts;
    private String marker;
    private EncodingType encodingType;

    public ListPartsRequest(String bucketName, String key, String uploadId) {
        super(Method.GET, bucketName, key, null);
        this.uploadId = uploadId;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        queryParams.put(S3Constants.PARAM_UPLOAD_ID, uploadId);
        if (maxParts != null) queryParams.put(S3Constants.PARAM_MAX_PARTS, maxParts.toString());
        if (marker != null) queryParams.put(S3Constants.PARAM_PART_NUMBER_MARKER, marker);
        if (encodingType != null) queryParams.put(S3Constants.PARAM_ENCODING_TYPE, encodingType.toString());
        return queryParams;
    }

    public String getUploadId() {
        return uploadId;
    }

    public Integer getMaxParts() {
        return maxParts;
    }

    public void setMaxParts(Integer maxParts) {
        this.maxParts = maxParts;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }
}
