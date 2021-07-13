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

    public void setStatus(String status) {
        this.status = status;
    }

    public ObjectLockLegalHold withStatus(String status) {
        setStatus(status);
        return this;
    }

    public void setStatus(Status status) { setStatus(status.toString()); }

    public ObjectLockLegalHold withStatus(Status status) {
        return withStatus(status.toString());
    }

    @XmlEnum
    public enum Status {
        ON, OFF
    }
}
