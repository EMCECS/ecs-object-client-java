/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3.bean;

import com.emc.object.util.Iso8601DateAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@XmlType(propOrder = {"id", "prefix", "status", "expiration", "noncurrentVersionExpiration"})
public class LifecycleRule {
    private String id;
    private String prefix;
    private Status status;
    private Expiration expiration;
    private NoncurrentVersionExpiration noncurrentVersionExpiration;

    public LifecycleRule() {
        this(null, null, null);
    }

    public LifecycleRule(String id, String prefix, Status status) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
    }

    /**
     * @see #withExpirationDays(Integer)
     * @deprecated please use builder methods instead
     */
    @Deprecated
    public LifecycleRule(String id, String prefix, Status status, Integer expirationDays) {
        this(id, prefix, status);
        setExpirationDays(expirationDays);
    }

    /**
     * @see #withExpirationDate(Date)
     * @deprecated please use builder methods instead
     */
    @Deprecated
    public LifecycleRule(String id, String prefix, Status status, Date expirationDate) {
        this(id, prefix, status);
        setExpirationDate(expirationDate);
    }

    public LifecycleRule withId(String id) {
        setId(id);
        return this;
    }

    public LifecycleRule withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    public LifecycleRule withStatus(Status status) {
        setStatus(status);
        return this;
    }

    public LifecycleRule withExpirationDays(Integer expirationDays) {
        setExpirationDays(expirationDays);
        return this;
    }

    public LifecycleRule withExpirationDate(Date expirationDate) {
        setExpirationDate(expirationDate);
        return this;
    }

    public LifecycleRule withNoncurrentVersionExpirationDays(Integer days) {
        setNoncurrentVersionExpirationDays(days);
        return this;
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
        return (expiration == null) ? null : expiration.days;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expiration = new Expiration();
        this.expiration.days = expirationDays;
    }

    @XmlTransient
    public Date getExpirationDate() {
        return (expiration == null) ? null : expiration.date;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expiration = new Expiration();
        this.expiration.date = expirationDate;
    }

    @XmlElement(name = "NoncurrentVersionExpiration")
    protected NoncurrentVersionExpiration getNoncurrentVersionExpiration() {
        return noncurrentVersionExpiration;
    }

    protected void setNoncurrentVersionExpiration(NoncurrentVersionExpiration noncurrentVersionExpiration) {
        this.noncurrentVersionExpiration = noncurrentVersionExpiration;
    }

    @XmlTransient
    public Integer getNoncurrentVersionExpirationDays() {
        return (noncurrentVersionExpiration == null) ? null : noncurrentVersionExpiration.days;
    }

    public void setNoncurrentVersionExpirationDays(Integer noncurrentDays) {
        this.noncurrentVersionExpiration = new NoncurrentVersionExpiration();
        this.noncurrentVersionExpiration.days = noncurrentDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LifecycleRule that = (LifecycleRule) o;

        // use ID if set
        if (id != null) return id.equals(that.id);

        // otherwise use everything else
        if (getExpirationDate() != null ? !getExpirationDate().equals(that.getExpirationDate()) : that.getExpirationDate() != null)
            return false;
        if (getExpirationDays() != null ? !getExpirationDays().equals(that.getExpirationDays()) : that.getExpirationDays() != null)
            return false;
        if (getNoncurrentVersionExpirationDays() != null ? !getNoncurrentVersionExpirationDays().equals(that.getNoncurrentVersionExpirationDays()) : that.getNoncurrentVersionExpirationDays() != null)
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
        result = 31 * result + (getExpirationDays() != null ? getExpirationDays().hashCode() : 0);
        result = 31 * result + (getExpirationDate() != null ? getExpirationDate().hashCode() : 0);
        result = 31 * result + (getNoncurrentVersionExpirationDays() != null ? getNoncurrentVersionExpirationDays().hashCode() : 0);
        return result;
    }

    @XmlEnum
    public static enum Status {
        Enabled, Disabled
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class Expiration {
        @XmlElement(name = "Days")
        public Integer days;
        @XmlElement(name = "Date")
        @XmlJavaTypeAdapter(value = Iso8601DateAdapter.class)
        public Date date;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class NoncurrentVersionExpiration {
        @XmlElement(name = "NoncurrentDays")
        public Integer days;
    }
}
