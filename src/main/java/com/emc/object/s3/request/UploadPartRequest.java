package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class UploadPartRequest<T> extends S3ObjectRequest implements EntityRequest<T> {
    private String uploadId;
    private int partNumber;
    private T object;
    private Long contentLength;
    private String contentMd5;

    public UploadPartRequest(String bucketName, String key, String uploadId, int partNumber, T object) {
        super(Method.PUT, bucketName, key, null);
    }

    @Override
    public Map<String, Object> getQueryParams() {
        Map<String, Object> queryParams = super.getQueryParams();
        queryParams.put(S3Constants.PARAM_UPLOAD_ID, partNumber);
        queryParams.put(S3Constants.PARAM_PART_NUMBER, partNumber);
        return queryParams;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (contentMd5 != null) RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_MD5, contentMd5);
        return headers;
    }

    @Override
    public T getEntity() {
        return getObject();
    }

    @Override
    public String getContentType() {
        return null;
    }

    public String getUploadId() {
        return uploadId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public T getObject() {
        return object;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public UploadPartRequest withContentLength(Long contentLength) {
        setContentLength(contentLength);
        return this;
    }

    public UploadPartRequest withContentMd5(String contentMd5) {
        setContentMd5(contentMd5);
        return this;
    }
}
