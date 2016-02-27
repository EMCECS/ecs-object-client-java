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
import com.emc.object.util.RestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryObjectsRequest extends AbstractBucketRequest {
    private Integer maxKeys;
    private String marker;
    private String query;
    private List<String> attributes;
    private String sorted;
    private boolean includeOlderVersions = false;

    public QueryObjectsRequest(String bucketName) {
        super(Method.GET, bucketName, "", null);
    }

    @Override
    public Map<String, String> getQueryParams() {
        Map<String, String> paramMap = super.getQueryParams();
        if (maxKeys != null) paramMap.put(S3Constants.PARAM_MAX_KEYS, maxKeys.toString());
        if (marker != null) paramMap.put(S3Constants.PARAM_MARKER, marker);
        if (query != null) paramMap.put(S3Constants.PARAM_QUERY, query);
        if (attributes != null && attributes.size() >= 1) paramMap.put(S3Constants.PARAM_ATTRIBUTES, formatAttributes(attributes));
        if (sorted != null) paramMap.put(S3Constants.PARAM_SORTED, sorted);
        if (includeOlderVersions) paramMap.put(S3Constants.PARAM_INCLUDE_OLDER_VERSIONS, "true");
        return paramMap;
    }

    private static String formatAttributes(List<String> attributes) {
        return RestUtil.join(",", attributes);
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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public String getSorted() {
        return sorted;
    }

    public void setSorted(String sorted) {
        this.sorted = sorted;
    }

    public boolean getIncludeOlderVersions() {
        return includeOlderVersions;
    }

    public void setIncludeOlderVersions(boolean includeOlderVersions) { this.includeOlderVersions = includeOlderVersions; }

    public QueryObjectsRequest withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }

    public QueryObjectsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }

    public QueryObjectsRequest withQuery(String query) {
        setQuery(query);
        return this;
    }

    public QueryObjectsRequest withAttributes(List<String> attributes) {
        setAttributes(attributes);
        return this;
    }

    public synchronized QueryObjectsRequest withAttribute(String attribute) {
        List<String> attributes = getAttributes();
        if(attributes == null) attributes = new ArrayList<String>();
        attributes.add(attribute);
        setAttributes(attributes);
        return this;
    }

    public QueryObjectsRequest withSorted(String sorted) {
        setSorted(sorted);
        return this;
    }

    public QueryObjectsRequest withIncludeOlderVersions(boolean includeOlderVersions) {
        setIncludeOlderVersions(includeOlderVersions);
        return this;
    }
}
