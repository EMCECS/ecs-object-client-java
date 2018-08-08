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
import com.emc.object.s3.bean.EncodingType;
import com.emc.object.util.RestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * <code>GET /?query={expression}&attributes={name, ...}&sorted={key}&include-older-versions={true|false}</code>
 * </p><p>
 * Executes a bucket search and returns list of objects, and their system and user metadata values, that match the selection conditions in the search query expression.
 * </p><p>
 * The objects returned can be restricted using the max-keys parameter. Where a subset of the search matches are returned, subsequent pages can be retrieved by using the marker from the previous search; the objects returned will start with the key after the marker.
 * </p>
 * <h2>Query Parameters</h2>
 * <p>
 * query (required) is an expression takes the form: <code>[(]{condition1}[%20[and/or]%20{condition2}][)][%20[and/or]%20...]</code>
 * </p><p>
 * where:
 * </p><p>
 * condition is a metadata keyname filter in the form: <code>{selector} {operator} {argument}</code>, for example <code>"LastModified>2015-01-01T00:00:00Z"</code>
 * in which:
 * <ul>
 * <li><code>selector</code> is a searchable keyname associated with the bucket</li>
 * <li><code>operator</code> is one of: ==, >, <, >=, <=</li>
 * <li><code>argument</code> is a value against which the selector is tested. The form of the argument must match the datatype of the key being indexed, which is one of: string, integer, datetime, decimal</li>
 * </ul>
 * </p>
 * <h2>Other Parameters</h2>
 * <ul>
 * <li><code>attributes</code> (optional) is a list one or more metadata names that are not being indexed, but which can be listed in the query results. For example: "&attributes=ContentType,Retention"</li>
 * <li><code>sorted</code> (optional) is the name of one key that appears in the query expression that becomes the sort key for the query results. If this optional parameter is absent, the sort order is the first keyname that appears in the expression.</li>
 * <li><code>include-older-versions</code> (optional) is a boolean that when set to true causes both current and non-current versions of the keys to be listed, and when set to false causes only the current versions of keys to be listed. The default is false.</li>
 * <li><code>max-keys</code> (optional) specifies the number of keys returned in the response body. Allows you to return fewer keys that the default.</li>
 * <li><code>marker</code> (optional) specifies the key to start with which will be the one after the marker. When no more pages exist, a marker will not be returned and NO MORE PAGES will be returned</li>
 * </ul>
 */
public class QueryObjectsRequest extends AbstractBucketRequest {
    private Integer maxKeys;
    private String marker;
    private String query;
    private List<String> attributes;
    private String sorted;
    private boolean includeOlderVersions = false;
    private EncodingType encodingType;

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
        if (encodingType != null) paramMap.put(S3Constants.PARAM_ENCODING_TYPE, encodingType.toString());
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

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }

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

    public QueryObjectsRequest withEncodingType(EncodingType encodingType) {
        setEncodingType(encodingType);
        return this;
    }
}
