package com.emc.object.s3.bean;


import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlRootElement(name = "Retention")
@XmlType(propOrder = {"mode", "retainUntilDate"})
public class ObjectLockRetention {
    private ObjectLockRetentionMode mode;
    private Date retainUntilDate;

    @XmlElement(name = "Mode")
    public ObjectLockRetentionMode getMode() {
        return mode;
    }

    public void setMode(ObjectLockRetentionMode mode) {
        this.mode = mode;
    }

    public ObjectLockRetention withMode(ObjectLockRetentionMode mode) {
        setMode(mode);
        return this;
    }

    @XmlElement(name = "RetainUntilDate")
    @XmlJavaTypeAdapter(Iso8601MillisecondAdapter.class)
    public Date getRetainUntilDate() {
        return retainUntilDate;
    }

    public void setRetainUntilDate(Date retainUntilDate) {
        this.retainUntilDate = retainUntilDate;
    }

    public ObjectLockRetention withRetainUntilDate(Date retainUntilDate) {
        setRetainUntilDate(retainUntilDate);
        return this;
    }
}

