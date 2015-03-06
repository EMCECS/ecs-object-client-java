/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
        this.uploadId = uploadId;
        this.partNumber = partNumber;
        this.object = object;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        queryParams.put(S3Constants.PARAM_UPLOAD_ID, uploadId);
        queryParams.put(S3Constants.PARAM_PART_NUMBER, Integer.toString(partNumber));
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
    public Long getContentLength() {
        return contentLength;
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
