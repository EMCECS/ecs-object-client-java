/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;

public class MultipartPartETag {
    private Integer partNumber;
    private String eTag;

    public MultipartPartETag() {
    }

    public MultipartPartETag(Integer partNumber, String eTag) {
        this.partNumber = partNumber;
        this.eTag = eTag;
    }

    @XmlElement(name = "PartNumber")
    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    @XmlElement(name = "ETag")
    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
}
