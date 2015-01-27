package com.emc.object.s3.request;

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;

import java.util.Map;

public class ListMultipartUploadsRequest extends AbstractBucketRequest {
    private String prefix;
    private String delimiter;
    private Integer maxUploads;
    private String keyMarker;
    private String uploadIdMarker;
    private EncodingType encodingType;

    public ListMultipartUploadsRequest(String bucketName) {
        super(Method.GET, bucketName, "", "uploads");
    }

    @Override
    public Map<String, Object> getQueryParams() {
        Map<String, Object> paramMap = super.getQueryParams();
        if (prefix != null) paramMap.put(S3Constants.PARAM_PREFIX, prefix);
        if (delimiter != null) paramMap.put(S3Constants.PARAM_DELIMITER, delimiter);
        if (maxUploads != null) paramMap.put(S3Constants.PARAM_MAX_UPLOADS, maxUploads);
        if (keyMarker != null) paramMap.put(S3Constants.PARAM_KEY_MARKER, keyMarker);
        if (uploadIdMarker != null) paramMap.put(S3Constants.PARAM_UPLOAD_ID_MARKER, uploadIdMarker);
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

    public Integer getMaxUploads() {
        return maxUploads;
    }

    public void setMaxUploads(Integer maxUploads) {
        this.maxUploads = maxUploads;
    }

    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    public void setUploadIdMarker(String uploadIdMarker) {
        this.uploadIdMarker = uploadIdMarker;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }

    public ListMultipartUploadsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    public ListMultipartUploadsRequest withDelimiter(String delimiter) {
        setDelimiter(delimiter);
        return this;
    }

    public ListMultipartUploadsRequest withMaxUploads(Integer maxUploads) {
        setMaxUploads(maxUploads);
        return this;
    }

    public ListMultipartUploadsRequest withKeyMarker(String keyMarker) {
        setKeyMarker(keyMarker);
        return this;
    }

    public ListMultipartUploadsRequest withUploadIdMarker(String uploadIdMarker) {
        setUploadIdMarker(uploadIdMarker);
        return this;
    }

    public ListMultipartUploadsRequest withEncodingType(EncodingType encodingType) {
        setEncodingType(encodingType);
        return this;
    }
}
