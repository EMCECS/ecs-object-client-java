package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;

import java.util.Map;

public class ListVersionsRequest extends AbstractBucketRequest {
    private String prefix;
    private String delimiter;
    private Integer maxKeys;
    private String keyMarker;
    private String versionIdMarker;
    private EncodingType encodingType;

    public ListVersionsRequest(String bucketName) {
        super(Method.GET, bucketName, "", "versions");
    }

    @Override
    public Map<String, Object> getQueryParams() {
        Map<String, Object> paramMap = super.getQueryParams();
        if (prefix != null) paramMap.put(S3Constants.PARAM_PREFIX, prefix);
        if (delimiter != null) paramMap.put(S3Constants.PARAM_DELIMITER, delimiter);
        if (maxKeys != null) paramMap.put(S3Constants.PARAM_MAX_KEYS, maxKeys);
        if (keyMarker != null) paramMap.put(S3Constants.PARAM_KEY_MARKER, keyMarker);
        if (versionIdMarker != null) paramMap.put(S3Constants.PARAM_VERSION_ID_MARKER, versionIdMarker);
        if (encodingType != null) paramMap.put(S3Constants.PARAM_ENCODING_TYPE, encodingType);
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

    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    public String getVersionIdMarker() {
        return versionIdMarker;
    }

    public void setVersionIdMarker(String versionIdMarker) {
        this.versionIdMarker = versionIdMarker;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }

    public ListVersionsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    public ListVersionsRequest withDelimiter(String delimiter) {
        setDelimiter(delimiter);
        return this;
    }

    public ListVersionsRequest withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }

    public ListVersionsRequest withKeyMarker(String keyMarker) {
        setKeyMarker(keyMarker);
        return this;
    }

    public ListVersionsRequest withVersionIdMarker(String versionIdMarker) {
        setVersionIdMarker(versionIdMarker);
        return this;
    }

    public ListVersionsRequest withEncodingType(EncodingType encodingType) {
        setEncodingType(encodingType);
        return this;
    }
}
