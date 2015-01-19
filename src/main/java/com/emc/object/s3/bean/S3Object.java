package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

public class S3Object {
    private String key;
    private Date lastModified;
    private String eTag;
    private Long size;
    private StorageClass storageClass;
    private CanonicalUser owner;

    @XmlElement(name = "Key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name = "LastModified")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @XmlElement(name = "ETag")
    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    @XmlElement(name = "Size")
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @XmlElement(name = "StorageClass")
    public StorageClass getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(StorageClass storageClass) {
        this.storageClass = storageClass;
    }

    @XmlElement(name = "Owner")
    public CanonicalUser getOwner() {
        return owner;
    }

    public void setOwner(CanonicalUser owner) {
        this.owner = owner;
    }
}
