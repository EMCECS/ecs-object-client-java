/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Deleted")
public class DeleteSuccess extends AbstractDeleteResult {
    private Boolean deleteMarker;
    private String deleteMarkerVersionId;

    @XmlElement(name = "DeleteMarker")
    public Boolean getDeleteMarker() {
        return deleteMarker;
    }

    public void setDeleteMarker(Boolean deleteMarker) {
        this.deleteMarker = deleteMarker;
    }

    @XmlElement(name = "DeleteMarkerVersionId")
    public String getDeleteMarkerVersionId() {
        return deleteMarkerVersionId;
    }

    public void setDeleteMarkerVersionId(String deleteMarkerVersionId) {
        this.deleteMarkerVersionId = deleteMarkerVersionId;
    }
}
