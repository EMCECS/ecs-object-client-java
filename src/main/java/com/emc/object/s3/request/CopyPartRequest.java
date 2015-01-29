package com.emc.object.s3.request;

import com.emc.object.Range;
import com.emc.object.s3.S3Constants;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CopyPartRequest extends CopyObjectRequest {
    private String uploadId;
    private int partNumber;
    private Range sourceRange;

    public CopyPartRequest(String sourceBucketName, String sourceKey, String bucketName, String key, String uploadId, int partNumber) {
        super(sourceBucketName, sourceKey, bucketName, key);
        this.uploadId = uploadId;
        this.partNumber = partNumber;
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
        if (sourceRange != null) RestUtil.putSingle(headers, S3Constants.AMZ_SOURCE_RANGE, "bytes=" + sourceRange);
        return headers;
    }

    public String getUploadId() {
        return uploadId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public Range getSourceRange() {
        return sourceRange;
    }

    public void setSourceRange(Range sourceRange) {
        this.sourceRange = sourceRange;
    }

    public CopyPartRequest withSourceRange(Range sourceRange) {
        setSourceRange(sourceRange);
        return this;
    }
}
