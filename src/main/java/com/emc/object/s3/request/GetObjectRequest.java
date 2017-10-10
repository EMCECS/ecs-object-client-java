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
import com.emc.object.Range;
import com.emc.object.util.RestUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetObjectRequest<T extends GetObjectRequest<T>> extends S3ObjectRequest {
    private String versionId;
    private Range range;
    private Date ifModifiedSince;
    private Date ifUnmodifiedSince;
    private String ifMatch;
    private String ifNoneMatch;
    private Map<ResponseHeaderOverride, String> headerOverrides = new HashMap<ResponseHeaderOverride, String>();

    public GetObjectRequest(String bucketName, String key) {
        this(Method.GET, bucketName, key);
    }

    protected GetObjectRequest(Method method, String bucketName, String key) {
        super(method, bucketName, key, null);
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> queryParams = super.getQueryParams();
        if (versionId != null) queryParams.put("versionId", versionId);
        for (ResponseHeaderOverride override : headerOverrides.keySet()) {
            queryParams.put(override.getQueryParam(), headerOverrides.get(override));
        }
        return queryParams;
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (range != null) RestUtil.putSingle(headers, RestUtil.HEADER_RANGE, "bytes=" + range.toString());
        if (ifModifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_MODIFIED_SINCE, RestUtil.headerFormat(ifModifiedSince));
        if (ifUnmodifiedSince != null)
            RestUtil.putSingle(headers, RestUtil.HEADER_IF_UNMODIFIED_SINCE, RestUtil.headerFormat(ifUnmodifiedSince));
        if (ifMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_MATCH, ifMatch);
        if (ifNoneMatch != null) RestUtil.putSingle(headers, RestUtil.HEADER_IF_NONE_MATCH, ifNoneMatch);
        return headers;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public Date getIfModifiedSince() {
        return ifModifiedSince;
    }

    public void setIfModifiedSince(Date ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
    }

    public Date getIfUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
        this.ifUnmodifiedSince = ifUnmodifiedSince;
    }

    public String getIfMatch() {
        return ifMatch;
    }

    public void setIfMatch(String ifMatch) {
        this.ifMatch = ifMatch;
    }

    public String getIfNoneMatch() {
        return ifNoneMatch;
    }

    public void setIfNoneMatch(String ifNoneMatch) {
        this.ifNoneMatch = ifNoneMatch;
    }

    public Map<ResponseHeaderOverride, String> getHeaderOverrides() {
        return headerOverrides;
    }

    public void setHeaderOverrides(Map<ResponseHeaderOverride, String> headerOverrides) {
        this.headerOverrides = headerOverrides;
    }

    @SuppressWarnings("unchecked")
    public T withVersionId(String versionId) {
        setVersionId(versionId);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withRange(Range range) {
        setRange(range);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withIfModifiedSince(Date ifModifiedSince) {
        setIfModifiedSince(ifModifiedSince);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withIfUnmodifiedSince(Date ifUnmodifiedSince) {
        setIfUnmodifiedSince(ifUnmodifiedSince);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withIfMatch(String ifMatch) {
        setIfMatch(ifMatch);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withIfNoneMatch(String ifNoneMatch) {
        setIfNoneMatch(ifNoneMatch);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T headerOverride(ResponseHeaderOverride override, String value) {
        headerOverrides.put(override, value);
        return (T) this;
    }
}
