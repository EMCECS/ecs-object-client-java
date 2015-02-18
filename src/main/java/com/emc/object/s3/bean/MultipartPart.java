package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

public class MultipartPart extends MultipartPartETag {
    private Date lastModified;
    private Long size;

    @XmlElement(name = "LastModified")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @XmlElement(name = "Size")
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
