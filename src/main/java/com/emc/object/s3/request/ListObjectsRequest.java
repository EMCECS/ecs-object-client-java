/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;

import java.util.Map;

public class ListObjectsRequest extends AbstractBucketRequest {
    private String prefix;
    private String delimiter;
    private Integer maxKeys;
    private String marker;
    private EncodingType encodingType;

    public ListObjectsRequest(String bucketName) {
        super(Method.GET, bucketName, "", null);
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> paramMap = super.getQueryParams();
        if (prefix != null) paramMap.put(S3Constants.PARAM_PREFIX, prefix);
        if (delimiter != null) paramMap.put(S3Constants.PARAM_DELIMITER, delimiter);
        if (maxKeys != null) paramMap.put(S3Constants.PARAM_MAX_KEYS, maxKeys.toString());
        if (marker != null) paramMap.put(S3Constants.PARAM_MARKER, marker);
        if (encodingType != null) paramMap.put(S3Constants.PARAM_ENCODING_TYPE, encodingType.toString());
        return paramMap;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public Integer getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
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

    public ListObjectsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    public ListObjectsRequest withDelimiter(String delimiter) {
        setDelimiter(delimiter);
        return this;
    }

    public ListObjectsRequest withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }

    public ListObjectsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }

    public ListObjectsRequest withEncodingType(EncodingType encodingType) {
        setEncodingType(encodingType);
        return this;
    }
}
