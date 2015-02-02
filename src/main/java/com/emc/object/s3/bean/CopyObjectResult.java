/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import com.emc.object.s3.S3Constants;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;

@XmlRootElement(name = "CopyObjectResult")
public class CopyObjectResult extends PutObjectResult {
    private Date lastModified;
    private String eTag;

    @XmlTransient
    public String getSourceVersionId() {
        return headerAsString(S3Constants.AMZ_SOURCE_VERSION_ID);
    }

    @XmlElement(name = "LastModified")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @XmlElement(name = "ETag")
    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
