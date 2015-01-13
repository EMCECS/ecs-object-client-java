package com.emc.object.s3.request;

public class ListObjectsRequest extends AbstractBucketRequest {
    private String prefix;
    private String delimiter;
    private String marker;
    private Integer maxKeys;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public Integer getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
    }

    public ListObjectsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    public ListObjectsRequest withDelimiter(String delimiter) {
        setDelimiter(delimiter);
        return this;
    }

    public ListObjectsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }

    public ListObjectsRequest withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }
}
