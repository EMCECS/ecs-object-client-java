package com.emc.object.s3.bean;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

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
