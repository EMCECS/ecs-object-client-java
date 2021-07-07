package com.emc.object.s3.bean;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "ObjectLockConfiguration")
@XmlType(propOrder = {"objectLockEnabled", "rule"})
public class ObjectLockConfiguration {
    private String objectLockEnabled;
    private ObjectLockRule rule;

    public ObjectLockConfiguration() {
    }

    @XmlElement(name = "ObjectLockEnabled")
    public String getObjectLockEnabled() { return this.objectLockEnabled; }

    public void setObjectLockEnabled(String objectLockEnabled) {
        this.withObjectLockEnabled(objectLockEnabled);
    }

    public ObjectLockConfiguration withObjectLockEnabled(String objectLockEnabled) {
        this.objectLockEnabled = objectLockEnabled;
        return this;
    }

    public ObjectLockConfiguration withObjectLockEnabled(ObjectLockEnabled objectLockEnabled) {
        return this.withObjectLockEnabled(objectLockEnabled.toString());
    }

    public void setObjectLockEnabled(ObjectLockEnabled objectLockEnabled) {
        this.setObjectLockEnabled(objectLockEnabled.toString());
    }

    @XmlElement(name = "Rule")
    public ObjectLockRule getRule() { return this.rule; }

    public void setRule(ObjectLockRule rule) {
        this.withRule(rule);
    }

    public ObjectLockConfiguration withRule(ObjectLockRule rule) {
        this.rule = rule;
        return this;
    }

    @XmlEnum
    public enum ObjectLockEnabled {
        Enabled, Disabled
    }
}
