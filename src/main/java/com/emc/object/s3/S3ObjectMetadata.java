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
package com.emc.object.s3;

import com.emc.object.util.RestUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3ObjectMetadata {
    public static final String EXPIRY_DATE = "expiry-date=";
    public static final String RULE_ID = "rule-id=";

    private String cacheControl;
    private String contentDisposition;
    private String contentEncoding;
    private Long contentLength;
    private String contentMd5;
    private String contentType;
    private String eTag;
    private Date expirationDate;
    private String expirationRuleId;
    private Date httpExpires;
    private Date lastModified;
    private String versionId;
    private Map<String, String> userMetadata = new HashMap<String, String>();

    public static <T> S3ObjectMetadata fromHeaders(Map<String, List<T>> headers) {
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.cacheControl = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CACHE_CONTROL);
        objectMetadata.contentDisposition = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_DISPOSITION);
        objectMetadata.contentEncoding = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_ENCODING);
        if (RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_LENGTH) != null)
            objectMetadata.contentLength = Long.parseLong(RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_LENGTH));
        objectMetadata.contentMd5 = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_MD5);
        objectMetadata.contentType = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_TYPE);
        objectMetadata.eTag = RestUtil.getFirstAsString(headers, RestUtil.HEADER_ETAG, true);
        objectMetadata.httpExpires = RestUtil.headerParse(
                RestUtil.stripQuotes(RestUtil.getFirstAsString(headers, RestUtil.HEADER_EXPIRES)));

        // prefer x-emc-mtime (has millisecond granularity)
        String mtime = RestUtil.getFirstAsString(headers, RestUtil.EMC_MTIME);
        if (mtime != null && mtime.length() > 0) objectMetadata.lastModified = new Date(Long.parseLong(mtime));
        else
            objectMetadata.lastModified = RestUtil.headerParse(RestUtil.getFirstAsString(headers, RestUtil.HEADER_LAST_MODIFIED));

        objectMetadata.versionId = RestUtil.getFirstAsString(headers, S3Constants.AMZ_VERSION_ID);
        objectMetadata.expirationDate = getExpirationDate(headers);
        objectMetadata.expirationRuleId = getExpirationRuleId(headers);
        objectMetadata.userMetadata = getUserMetadata(headers);
        return objectMetadata;
    }

    public static <T> Date getExpirationDate(Map<String, List<T>> headers) {
        List<T> expValues = headers.get(S3Constants.AMZ_EXPIRATION);
        if (expValues != null) {
            for (Object value : expValues) {
                if (value.toString().startsWith(EXPIRY_DATE)) {
                    String expString = value.toString().substring(EXPIRY_DATE.length()); // after equals sign
                    return RestUtil.headerParse(RestUtil.stripQuotes(expString)); // remove quotes
                }
            }
        }
        return null;
    }

    public static <T> String getExpirationRuleId(Map<String, List<T>> headers) {
        List<T> expValues = headers.get(S3Constants.AMZ_EXPIRATION);
        if (expValues != null) {
            for (Object value : expValues) {
                if (value.toString().startsWith(RULE_ID)) {
                    String expString = value.toString().substring(RULE_ID.length()); // after equals sign
                    return RestUtil.stripQuotes(expString); // remove quotes
                }
            }
        }
        return null;
    }

    public static <T> Map<String, String> getUserMetadata(Map<String, List<T>> headers) {
        Map<String, String> userMetadata = new HashMap<String, String>();
        for (String name : headers.keySet()) {
            String key = getUserMetadataKey(name);
            if (key != null) {
                userMetadata.put(key, RestUtil.getFirstAsString(headers, name));
            }
        }
        return userMetadata;
    }

    protected static String getUserMetadataKey(String headerName) {
        if (headerName.startsWith(S3Constants.AMZ_META_PREFIX)) {
            return headerName.substring(S3Constants.AMZ_META_PREFIX.length());
        }
        return null;
    }

    public Map<String, List<Object>> toHeaders() {
        Map<String, List<Object>> headers = new HashMap<String, List<Object>>();
        RestUtil.putSingle(headers, RestUtil.HEADER_CACHE_CONTROL, cacheControl);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_DISPOSITION, contentDisposition);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_ENCODING, contentEncoding);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_MD5, contentMd5);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_TYPE, contentType);
        RestUtil.putSingle(headers, RestUtil.HEADER_EXPIRES, RestUtil.headerFormat(httpExpires));
        for (String name : userMetadata.keySet()) {
            RestUtil.putSingle(headers, getHeaderName(name), userMetadata.get(name));
        }
        return headers;
    }

    public static String getHeaderName(String userMetadataKey) {
        return S3Constants.AMZ_META_PREFIX + userMetadataKey;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getExpirationRuleId() {
        return expirationRuleId;
    }

    public void setExpirationRuleId(String expirationRuleId) {
        this.expirationRuleId = expirationRuleId;
    }

    public Date getHttpExpires() {
        return httpExpires;
    }

    public void setHttpExpires(Date httpExpires) {
        this.httpExpires = httpExpires;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(Map<String, String> userMetadata) {
        this.userMetadata = userMetadata;
    }

    public String getUserMetadata(String name) {
        return userMetadata.get(name);
    }

    public String getDecodedUserMetadata(String name) {
        return RestUtil.urlDecode(getUserMetadata(name));
    }

    public S3ObjectMetadata addUserMetadata(String name, String value) {
        userMetadata.put(name, value);
        return this;
    }

    public S3ObjectMetadata addEncodedUserMetadata(String name, String value) {
        return addUserMetadata(name, RestUtil.urlEncode(value));
    }

    public S3ObjectMetadata withContentType(String contentType) {
        setContentType(contentType);
        return this;
    }

    public S3ObjectMetadata withContentLength(Long contentLength) {
        setContentLength(contentLength);
        return this;
    }

    public S3ObjectMetadata withContentLength(int contentLength) {
        return withContentLength((long) contentLength);
    }


    public S3ObjectMetadata withContentMd5(String contentMd5) {
        setContentMd5(contentMd5);
        return this;
    }

    public S3ObjectMetadata withContentDisposition(String contentDisposition) {
        setContentDisposition(contentDisposition);
        return this;
    }

    public S3ObjectMetadata withContentEncoding(String contentEncoding) {
        setContentEncoding(contentEncoding);
        return this;
    }

    public S3ObjectMetadata withCacheControl(String cacheControl) {
        setCacheControl(cacheControl);
        return this;
    }

    public S3ObjectMetadata withHttpExpires(Date httpExpires) {
        setHttpExpires(httpExpires);
        return this;
    }
}
