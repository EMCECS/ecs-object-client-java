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

    public void setMode(String mode) {
        this.mode = mode;
    }

    public DefaultRetention withMode(String mode) {
        setMode(mode);
        return this;
    }

    public void setMode(ObjectLockRetentionMode mode) {
        setMode(mode.toString());
    }

    public DefaultRetention withMode(ObjectLockRetentionMode mode) {
        return withMode(mode.toString());
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