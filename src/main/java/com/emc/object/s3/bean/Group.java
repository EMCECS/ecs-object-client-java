/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Group")
public class Group extends AbstractGrantee {
    private String uri;

    public Group() {
    }

    public Group(String uri) {
        this.uri = uri;
    }

    @XmlElement(name = "URI")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getHeaderValue() {
        return "uri=\"" + getUri() + "\"";
    }

    @Override
    public int compareTo(AbstractGrantee o) {
        if (o instanceof Group)
            return uri.compareTo(((Group) o).getUri());
        else
            return super.compareTo(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Group group = (Group) o;

        if (uri != null ? !uri.equals(group.uri) : group.uri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }
}
