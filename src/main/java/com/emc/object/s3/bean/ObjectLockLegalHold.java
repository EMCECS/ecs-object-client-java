package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "LegalHold")
@XmlType(propOrder = {"status"})
public class ObjectLockLegalHold {
    private Status status;

    @XmlElement(name = "Status")
    public Status getStatus() { return status; }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ObjectLockLegalHold withStatus(Status status) {
        setStatus(status);
        return this;
    }

    @XmlEnum
    public enum Status {
        ON, OFF
    }
}
