package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "LegalHold")
@XmlType(propOrder = {"status"})
public class ObjectLockLegalHold {
    private String status;

    @XmlElement(name = "Status")
    public String getStatus() { return status; }

    public ObjectLockLegalHold withStatus(String status) {
        this.status = status;
        return this;
    }

    public ObjectLockLegalHold withStatus(Status status) {
        this.status = status.toString();
        return this;
    }

    public void setStatus(String status) {
        withStatus(status);
    }

    public void setStatus(Status status) {
        withStatus(status);
    }

    @XmlEnum
    public enum Status {
        ON, OFF
    }

}
