package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(propOrder = {"mode", "days", "years"})
/**
 * DefaultRetention requires both a mode and a period. The period can be either Days or Years, not both.
 */
public class DefaultRetention implements Serializable {
    private String mode;
    private Integer days;
    private Integer years;

    public DefaultRetention() {
    }

    @XmlElement(name = "Mode")
    public String getMode() {
        return this.mode;
    }

    public DefaultRetention withMode(String mode) {
        this.mode = mode;
        return this;
    }

    public DefaultRetention withMode(ObjectLockRetentionMode mode) {
        return this.withMode(mode.toString());
    }

    public void setMode(ObjectLockRetentionMode mode) {
        this.withMode(mode);
    }

    public void setMode(String mode) {
        this.withMode(mode);
    }

    @XmlElement(name = "Days")
    public Integer getDays() {
        return this.days;
    }

    public DefaultRetention withDays(Integer days) {
        this.days = days;
        return this;
    }

    public void setDays(Integer days) {
        this.withDays(days);
    }

    @XmlElement(name = "Years")
    public Integer getYears() {
        return this.years;
    }

    public DefaultRetention withYears(Integer years) {
        this.years = years;
        return this;
    }

    public void setYears(Integer years) {
        this.withYears(years);
    }
}