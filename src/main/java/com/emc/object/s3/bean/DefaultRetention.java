package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * DefaultRetention requires both a mode and a period. The period can be either Days or Years, not both.
 */
@XmlType(propOrder = {"mode", "days", "years"})
public class DefaultRetention implements Serializable {
    private ObjectLockRetentionMode mode;
    private Integer days;
    private Integer years;

    public DefaultRetention() {
    }

    @XmlElement(name = "Mode")
    public ObjectLockRetentionMode getMode() {
        return this.mode;
    }

    public void setMode(ObjectLockRetentionMode mode) {
        this.mode = mode;
    }

    public DefaultRetention withMode(ObjectLockRetentionMode mode) {
        setMode(mode);
        return this;
    }

    @XmlElement(name = "Days")
    public Integer getDays() {
        return this.days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public DefaultRetention withDays(Integer days) {
        setDays(days);
        return this;
    }

    @XmlElement(name = "Years")
    public Integer getYears() {
        return this.years;
    }

    public void setYears(Integer years) {
        this.years = years;
    }

    public DefaultRetention withYears(Integer years) {
        setYears(years);
        return this;
    }
}