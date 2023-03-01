package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "ObjectLockConfiguration")
@XmlType(propOrder = {"objectLockEnabled", "rule"})
public class ObjectLockConfiguration {
    private ObjectLockEnabled objectLockEnabled;
    private ObjectLockRule rule;

    public ObjectLockConfiguration() {
    }

    @XmlElement(name = "ObjectLockEnabled")
    public ObjectLockEnabled getObjectLockEnabled() {
        return this.objectLockEnabled;
    }

    public void setObjectLockEnabled(ObjectLockEnabled objectLockEnabled) {
        this.objectLockEnabled = objectLockEnabled;
    }

    public ObjectLockConfiguration withObjectLockEnabled(ObjectLockEnabled objectLockEnabled) {
        setObjectLockEnabled(objectLockEnabled);
        return this;
    }

    @XmlElement(name = "Rule")
    public ObjectLockRule getRule() {
        return this.rule;
    }

    public void setRule(ObjectLockRule rule) {
        this.rule = rule;
    }

    public ObjectLockConfiguration withRule(ObjectLockRule rule) {
        setRule(rule);
        return this;
    }

    @XmlEnum
    public enum ObjectLockEnabled {
        Enabled
    }
}
