package com.emc.object.s3.bean;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(propOrder = {"defaultRetention"})
public class ObjectLockRule implements Serializable {
    private DefaultRetention defaultRetention;

    @XmlElement(name = "DefaultRetention")
    public DefaultRetention getDefaultRetention() {
        return defaultRetention;
    }

    public void setDefaultRetention(DefaultRetention defaultRetention) {
        this.defaultRetention = defaultRetention;
    }

    public ObjectLockRule withDefaultRetention(DefaultRetention defaultRetention) {
        setDefaultRetention(defaultRetention);
        return this;
    }
}