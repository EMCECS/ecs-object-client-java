/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
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
    private Map<String, String> userMetadata = new HashMap<>();

    public static <T> S3ObjectMetadata fromHeaders(Map<String, List<T>> headers) {
        S3ObjectMetadata objectMetadata = new S3ObjectMetadata();
        objectMetadata.cacheControl = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CACHE_CONTROL);
        objectMetadata.contentDisposition = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_DISPOSITION);
        objectMetadata.contentEncoding = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_ENCODING);
        objectMetadata.contentLength = Long.parseLong(RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_LENGTH));
        objectMetadata.contentMd5 = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_MD5);
        objectMetadata.contentType = RestUtil.getFirstAsString(headers, RestUtil.HEADER_CONTENT_TYPE);
        objectMetadata.eTag = RestUtil.getFirstAsString(headers, RestUtil.HEADER_ETAG);
        objectMetadata.httpExpires = RestUtil.headerParse(RestUtil.getFirstAsString(headers, RestUtil.HEADER_EXPIRES));
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
                    expString = expString.replaceFirst("^\"", "").replaceFirst("\"$", ""); // remove quotes
                    return RestUtil.headerParse(expString);
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
                    return expString.replaceFirst("^\"", "").replaceFirst("\"$", ""); // remove quotes
                }
            }
        }
        return null;
    }

    public static <T> Map<String, String> getUserMetadata(Map<String, List<T>> headers) {
        Map<String, String> userMetadata = new HashMap<>();
        for (String name : headers.keySet()) {
            if (name.startsWith(S3Constants.AMZ_META_PREFIX)) {
                userMetadata.put(name.substring(S3Constants.AMZ_META_PREFIX.length()),
                        RestUtil.getFirstAsString(headers, name));
            }
        }
        return userMetadata;
    }

    public Map<String, List<Object>> toHeaders() {
        Map<String, List<Object>> headers = new HashMap<>();
        RestUtil.putSingle(headers, RestUtil.HEADER_CACHE_CONTROL, cacheControl);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_DISPOSITION, contentDisposition);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_ENCODING, contentEncoding);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_LENGTH, contentLength);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_MD5, contentMd5);
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_TYPE, contentType);
        for (String name : userMetadata.keySet()) {
            RestUtil.putSingle(headers, S3Constants.AMZ_META_PREFIX + name, userMetadata.get(name));
        }
        return headers;
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

    public String userMetadata(String name) {
        return userMetadata.get(name);
    }

    public S3ObjectMetadata userMetadata(String name, String value) {
        userMetadata.put(name, value);
        return this;
    }

    public S3ObjectMetadata withContentType(String contentType) {
        setContentType(contentType);
        return this;
    }

    public S3ObjectMetadata withContentLength(Long contentLength) {
        setContentLength(contentLength);
        return this;
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
