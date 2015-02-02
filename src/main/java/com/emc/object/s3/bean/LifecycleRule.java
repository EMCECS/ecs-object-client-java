/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.*;
import java.util.Date;

@XmlType(propOrder = {"id", "prefix", "status", "expiration"})
public class LifecycleRule {
    private String id;
    private String prefix;
    private Status status;
    private Expiration expiration;

    public LifecycleRule() {
        this(null, null, null, null, null);
    }

    public LifecycleRule(String id, String prefix, Status status, Integer expirationDays) {
        this(id, prefix, status, expirationDays, null);
    }

    public LifecycleRule(String id, String prefix, Status status, Date expirationDate) {
        this(id, prefix, status, null, expirationDate);
    }

    private LifecycleRule(String id, String prefix, Status status, Integer expirationDays, Date expirationDate) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expiration = new Expiration();
        this.expiration.days = expirationDays;
        this.expiration.date = expirationDate;
    }

    @XmlElement(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Status")
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @XmlElement(name = "Expiration")
    protected Expiration getExpiration() {
        return expiration;
    }

    protected void setExpiration(Expiration expiration) {
        this.expiration = expiration;
    }

    @XmlTransient
    public Integer getExpirationDays() {
        return expiration.days;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expiration.days = expirationDays;
    }

    @XmlTransient
    public Date getExpirationDate() {
        return expiration.date;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expiration.date = expirationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LifecycleRule that = (LifecycleRule) o;

        // use ID if set
        if (id != null) return id.equals(that.id);

        // otherwise use everything else
        if (expiration.date != null ? !expiration.date.equals(that.expiration.date) : that.expiration.date != null)
            return false;
        if (expiration.days != null ? !expiration.days.equals(that.expiration.days) : that.expiration.days != null)
            return false;
        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) return false;
        if (status != that.status) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // use ID if set
        if (id != null) return id.hashCode();

        // otherwise use everything else
        int result = prefix != null ? prefix.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (expiration.days != null ? expiration.days.hashCode() : 0);
        result = 31 * result + (expiration.date != null ? expiration.date.hashCode() : 0);
        return result;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class Expiration {
        @XmlElement(name = "Days")
        public Integer days;
        @XmlElement(name = "Date")
        public Date date;
    }

    @XmlEnum
    public static enum Status {
        Enabled, Disabled
    }
}
