package com.emc.object.s3.bean;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlRootElement(name = "Retention")
@XmlType(propOrder = {"mode", "retainUntilDate"})
public class ObjectLockRetention {
    private String mode;
    private Date retainUntilDate;

    @XmlElement(name = "Mode")
    public String getMode() {
        return mode;
    }

    public ObjectLockRetention withMode(String mode) {
        this.mode = mode;
        return this;
    }

    public ObjectLockRetention withMode(ObjectLockRetentionMode mode) {
        return withMode(mode.toString());
    }

    public void setMode(String mode) {
        withMode(mode);
    }

    public void setMode(ObjectLockRetentionMode mode) {
        setMode(mode.toString());
    }

    @XmlElement(name = "RetainUntilDate")
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    public Date getRetainUntilDate() {
        return retainUntilDate;
    }

    public ObjectLockRetention withRetainUntilDate(Date retainUntilDate) {
        this.retainUntilDate = retainUntilDate;
        return this;
    }

    public void setRetainUntilDate(Date retainUntilDate) {
        withRetainUntilDate(retainUntilDate);
    }



}

