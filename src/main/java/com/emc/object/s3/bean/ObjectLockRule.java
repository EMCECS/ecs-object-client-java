package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType(propOrder = {"defaultRetention"})
public class ObjectLockRule implements Serializable {
    private DefaultRetention defaultRetention;

    @XmlElement(name = "DefaultRetention")
    public DefaultRetention getDefaultRetention() {
        return defaultRetention;
    }

    public ObjectLockRule withDefaultRetention(DefaultRetention defaultRetention) {
        this.defaultRetention = defaultRetention;
        return this;
    }

    public void setDefaultRetention(DefaultRetention defaultRetention) {
        withDefaultRetention(defaultRetention);
    }
}