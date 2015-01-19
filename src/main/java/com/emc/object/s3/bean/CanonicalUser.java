package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "CanonicalUser", propOrder = {"id", "displayName"})
public class CanonicalUser extends AbstractGrantee {
    private String id;
    private String displayName;

    public CanonicalUser() {
    }

    public CanonicalUser(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @XmlElement(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "DisplayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getHeaderValue() {
        return "id=\"" + getId() + "\"";
    }

    @Override
    public int compareTo(AbstractGrantee o) {
        if (o instanceof CanonicalUser)
            return id.compareTo(((CanonicalUser) o).getId());
        else
            return super.compareTo(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CanonicalUser that = (CanonicalUser) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
