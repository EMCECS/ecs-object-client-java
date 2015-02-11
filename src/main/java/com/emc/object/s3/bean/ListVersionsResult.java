/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListVersionsResult")
@XmlType(propOrder = {"bucketName", "prefix", "keyMarker", "versionIdMarker", "nextKeyMarker", "nextVersionIdMarker", "maxKeys", "delimiter", "truncated", "versions", "_commonPrefixes"})
public class ListVersionsResult {
    private String bucketName;
    private String prefix;
    private String delimiter;
    private Integer maxKeys;
    private String keyMarker;
    private String versionIdMarker;
    private String nextKeyMarker;
    private String nextVersionIdMarker;
    private Boolean truncated;
    private List<AbstractVersion> versions = new ArrayList<AbstractVersion>();
    private List<CommonPrefix> _commonPrefixes = new ArrayList<CommonPrefix>();

    @XmlElement(name = "Name")
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

    @XmlElement(name = "MaxKeys")
    public Integer getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
    }

    @XmlElement(name = "KeyMarker")
    public String getKeyMarker() {
        return keyMarker;
    }

    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    @XmlElement(name = "VersionIdMarker")
    public String getVersionIdMarker() {
        return versionIdMarker;
    }

    public void setVersionIdMarker(String versionIdMarker) {
        this.versionIdMarker = versionIdMarker;
    }

    @XmlElement(name = "NextKeyMarker")
    public String getNextKeyMarker() {
        return nextKeyMarker;
    }

    public void setNextKeyMarker(String nextKeyMarker) {
        this.nextKeyMarker = nextKeyMarker;
    }

    @XmlElement(name = "NextVersionIdMarker")
    public String getNextVersionIdMarker() {
        return nextVersionIdMarker;
    }

    public void setNextVersionIdMarker(String nextVersionIdMarker) {
        this.nextVersionIdMarker = nextVersionIdMarker;
    }

    @XmlElement(name = "IsTruncated")
    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    @XmlElementRefs({@XmlElementRef(type = DeleteMarker.class), @XmlElementRef(type = Version.class)})
    public List<AbstractVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<AbstractVersion> versions) {
        this.versions = versions;
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
        List<String> prefixes = new ArrayList<String>();
        for (CommonPrefix prefix : _commonPrefixes) {
            prefixes.add(prefix.getPrefix());
        }
        return prefixes;
    }
}
