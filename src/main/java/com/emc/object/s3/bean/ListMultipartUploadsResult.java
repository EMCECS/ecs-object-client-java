package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListMultipartUploadsResult")
public class ListMultipartUploadsResult {
    private String bucketName;
    private String prefix;
    private String delimiter;
    private Integer maxUploads;
    private String keyMarker;
    private String uploadIdMarker;
    private String nextKeyMarker;
    private String nextUploadIdMarker;
    private Boolean truncated;
    private List<Upload> uploads = new ArrayList<>();
    private List<CommonPrefix> _commonPrefixes = new ArrayList<>();

    @XmlElement(name = "Bucket")
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Delimiter")
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @XmlElement(name = "MaxUploads")
    public Integer getMaxUploads() {
        return maxUploads;
    }

    public void setMaxUploads(Integer maxUploads) {
        this.maxUploads = maxUploads;
    }

    @XmlElement(name = "KeyMarker")
    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    @XmlElement(name = "UploadIdMarker")
    public String getUploadIdMarker() {
        return uploadIdMarker;
    }

    public void setUploadIdMarker(String uploadIdMarker) {
        this.uploadIdMarker = uploadIdMarker;
    }

    @XmlElement(name = "NextsKeyMarker")
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }

    @XmlElement(name = "NextUploadIdMarker")
    public String getNextUploadIdMarker() {
        return nextUploadIdMarker;
    }

    public void setNextUploadIdMarker(String nextUploadIdMarker) {
        this.nextUploadIdMarker = nextUploadIdMarker;
    }

    @XmlElement(name = "IsTruncated")
    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    @XmlElement(name = "Upload")
    public List<Upload> getUploads() {
        return uploads;
    }

    public void setUploads(List<Upload> uploads) {
        this.uploads = uploads;
    }

    @XmlElement(name = "CommonPrefixes")
    protected List<CommonPrefix> get_commonPrefixes() {
        return _commonPrefixes;
    }

    protected void set_commonPrefixes(List<CommonPrefix> _commonPrefixes) {
        this._commonPrefixes = _commonPrefixes;
    }

    @XmlTransient
    public List<String> getCommonPrefixes() {
        List<String> prefixes = new ArrayList<>();
        for (CommonPrefix prefix : _commonPrefixes) {
            prefixes.add(prefix.getPrefix());
        }
        return prefixes;
    }
}
