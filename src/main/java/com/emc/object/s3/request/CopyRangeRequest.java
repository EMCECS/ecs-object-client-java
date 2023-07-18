package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.CopyRange;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class CopyRangeRequest extends S3ObjectRequest implements EntityRequest {

    private String contentMd5;
    private String copyMode;
    private String multiPartCopy;
    private CopyRange copyRange;

    public CopyRangeRequest(String bucketName, String key) {
        super(Method.PUT, bucketName, key, null);
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (contentMd5 != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_MD5, contentMd5);
        if (copyMode != null)
            RestUtil.putSingle(headers, RestUtil.EMC_COPY_MODE, copyMode);
        if (multiPartCopy != null)
            RestUtil.putSingle(headers, RestUtil.EMC_MULTIPART_COPY, multiPartCopy);
       return headers;
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    @Override
    public Long getContentLength() {
        return null; // assume chunked encoding or buffering
    }

    @Override
    public boolean isChunkable() {
        return false;
    }

    @Override
    public Object getEntity() {
        return copyRange;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public String getCopyMode() {
        return copyMode;
    }

    public void setCopyMode(String copyMode) {
        this.copyMode = copyMode;
    }

    public String getMultiPartCopy() {
        return multiPartCopy;
    }

    public void setMultiPartCopy(String multiPartCopy) {
        this.multiPartCopy = multiPartCopy;
    }

    public CopyRange getCopyRange() {
        return copyRange;
    }

    public void setCopyRange(CopyRange copyRange) {
        this.copyRange = copyRange;
    }

    public CopyRangeRequest withContentMd5(String contentMd5) {
        setContentMd5(contentMd5);
        return this;
    }

    public CopyRangeRequest withCopyMode(String copyMode) {
        setCopyMode(copyMode);
        return this;
    }

    public CopyRangeRequest withMultiPartCopy(String multiPartCopy) {
        setMultiPartCopy(multiPartCopy);
        return this;
    }

    public CopyRangeRequest withCopyRange(CopyRange copyRange) {
        setCopyRange(copyRange);
        return this;
    }

}
