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

    public void setMode(String mode) {
        this.mode = mode;
    }

    public ObjectLockRetention withMode(String mode) {
        setMode(mode);
        return this;
    }

    public void setMode(ObjectLockRetentionMode mode) { setMode(mode.toString()); }

    public ObjectLockRetention withMode(ObjectLockRetentionMode mode) {
        return withMode(mode.toString());
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

