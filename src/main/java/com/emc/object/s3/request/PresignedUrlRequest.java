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

import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3ObjectMetadata;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PresignedUrlRequest extends S3ObjectRequest {
    private Date expirationTime;
    private Method method;
    private String versionId;
    private S3ObjectMetadata objectMetadata;
    private Map<ResponseHeaderOverride, String> headerOverrides = new HashMap<ResponseHeaderOverride, String>();

    public PresignedUrlRequest(Method method, String bucketName, String key, Date expirationTime) {
        super(method, bucketName, key, null);
        this.method = method;
        this.expirationTime = expirationTime;
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put(S3Constants.PARAM_VERSION_ID, versionId);
        queryParams.put(S3Constants.PARAM_EXPIRES, Long.toString(expirationTime.getTime() / 1000));
        for (ResponseHeaderOverride override : headerOverrides.keySet()) {
            queryParams.put(override.getQueryParam(), headerOverrides.get(override));
        }
        return queryParams;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (objectMetadata != null) headers.putAll(objectMetadata.toHeaders());
        return headers;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public Method getMethod() {
        return method;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public S3ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(S3ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public Map<ResponseHeaderOverride, String> getHeaderOverrides() {
        return headerOverrides;
    }

    public void setHeaderOverrides(Map<ResponseHeaderOverride, String> headerOverrides) {
        this.headerOverrides = headerOverrides;
    }

    public PresignedUrlRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

    public PresignedUrlRequest withObjectMetadata(S3ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public PresignedUrlRequest headerOverride(ResponseHeaderOverride override, String value) {
        headerOverrides.put(override, value);
        return this;
    }
}
