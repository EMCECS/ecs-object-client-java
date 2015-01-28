package com.emc.object.s3;

import com.emc.object.util.RestUtil;

import java.util.*;

public class S3ObjectMetadata {
    public static final String EXPIRY_DATE = "expiry-date=";
    public static final String RULE_ID = "rule-id=";

    private Map<String, List<Object>> headers;

    public static S3ObjectMetadata fromHeaders(Map<String, List<Object>> headers) {
        S3ObjectMetadata metadata = new S3ObjectMetadata();
        for (String header : headers.keySet()) {
            metadata.headers.put(header, new ArrayList<>(headers.get(header)));
        }
        return metadata;
    }

    public static Date getExpirationDate(Map<String, List<Object>> headers) {
        List<Object> expValues = headers.get(S3Constants.AMZ_EXPIRATION);
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

    public static String getExpirationRuleId(Map<String, List<Object>> headers) {
        List<Object> expValues = headers.get(S3Constants.AMZ_EXPIRATION);
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

    public S3ObjectMetadata() {
        headers = new HashMap<>();
    }

    public Map<String, List<Object>> toHeaders() {
        Map<String, List<Object>> clone = new HashMap<>();
        for (String header : headers.keySet()) {
            clone.put(header, new ArrayList<>(headers.get(header)));
        }
        return clone;
    }

    public String getContentType() {
        return RestUtil.getFirst(headers, RestUtil.HEADER_CONTENT_TYPE).toString();
    }

    public void setContentType(String contentType) {
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_TYPE, contentType);
    }

    public Long getContentLength() {
        return Long.parseLong(RestUtil.getFirst(headers, RestUtil.HEADER_CONTENT_LENGTH).toString());
    }

    public void setContentLength(Long contentLength) {
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_LENGTH, contentLength.toString());
    }

    public Date getLastModified() {
        return RestUtil.headerParse(RestUtil.getFirst(headers, RestUtil.HEADER_LAST_MODIFIED).toString());
    }

    public String getETag() {
        return RestUtil.getFirst(headers, RestUtil.HEADER_ETAG).toString();
    }

    public String getContentMd5() {
        return RestUtil.getFirst(headers, RestUtil.HEADER_CONTENT_MD5).toString();
    }

    public void setContentMd5(String contentMd5) {
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_MD5, contentMd5);
    }

    public String getContentDisposition() {
        return RestUtil.getFirst(headers, RestUtil.HEADER_CONTENT_DISPOSITION).toString();
    }

    public void setContentDisposition(String contentDisposition) {
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_DISPOSITION, contentDisposition);
    }

    public String getContentEncoding() {
        return RestUtil.getFirst(headers, RestUtil.HEADER_CONTENT_ENCODING).toString();
    }

    public void setContentEncoding(String contentEncoding) {
        RestUtil.putSingle(headers, RestUtil.HEADER_CONTENT_ENCODING, contentEncoding);
    }

    public String getCacheControl() {
        return RestUtil.getFirst(headers, RestUtil.HEADER_CACHE_CONTROL).toString();
    }

    public void setCacheControl(String cacheControl) {
        RestUtil.putSingle(headers, RestUtil.HEADER_CACHE_CONTROL, cacheControl);
    }

    public Date getHttpExpires() {
        return RestUtil.headerParse(RestUtil.getFirst(headers, RestUtil.HEADER_EXPIRES).toString());
    }

    public void setHttpExpires(Date httpExpires) {
        RestUtil.putSingle(headers, RestUtil.HEADER_EXPIRES, RestUtil.headerFormat(httpExpires));
    }

    public String getVersionId() {
        return RestUtil.getFirst(headers, S3Constants.AMZ_VERSION_ID).toString();
    }

    public Date getExpirationTime() {
        return getExpirationDate(headers);
    }

    public String getExpirationRuleId() {
        return getExpirationRuleId(headers);
    }

    public List<String> userMetadataKeys() {
        List<String> metaKeys = new ArrayList<>();
        for (String header : headers.keySet()) {
            if (header.startsWith(S3Constants.AMZ_META_PREFIX))
                metaKeys.add(header.substring(S3Constants.AMZ_META_PREFIX.length()));
        }
        return metaKeys;
    }

    public String userMetadata(String name) {
        return RestUtil.getFirst(headers, S3Constants.AMZ_META_PREFIX + name).toString();
    }

    public void userMetadata(String name, String value) {
        RestUtil.putSingle(headers, S3Constants.AMZ_META_PREFIX + name, value);
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

    public S3ObjectMetadata withUserMeta(String name, String value) {
        userMetadata(name, value);
        return this;
    }
}
