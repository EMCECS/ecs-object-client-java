package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;

public abstract class AbstractVersion {
    private String key;
    private String versionId;
    private Boolean latest;
    private Date lastModified;
    private Owner owner;

    @XmlElement(name = "Key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name = "VersionId")
    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    @XmlElement(name = "IsLatest")
    public Boolean getLatest() {
        return latest;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest;
    }

    @XmlElement(name = "LastModified")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @XmlElement(name = "Owner")
    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }
}
