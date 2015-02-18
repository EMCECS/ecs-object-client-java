/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "CompleteMultipartUpload")
public class CompleteMultipartUpload {
    private List<MultipartPartETag> parts = new ArrayList<MultipartPartETag>();

    public CompleteMultipartUpload() {
    }

    public CompleteMultipartUpload(List<MultipartPartETag> parts) {
        this.parts = parts;
    }

    @XmlElement(name = "Part")
    public List<MultipartPartETag> getParts() {
        return parts;
    }

    public void setParts(List<MultipartPartETag> parts) {
        this.parts = parts;
    }
}
