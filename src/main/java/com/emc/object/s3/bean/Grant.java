package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"grantee", "permission"})
public class Grant implements Comparable<Grant> {
    private AbstractGrantee grantee;
    private Permission permission;

    public Grant() {
    }

    public Grant(AbstractGrantee grantee, Permission permission) {
        this.grantee = grantee;
        this.permission = permission;
    }

    @XmlElement(name = "Grantee")
    public AbstractGrantee getGrantee() {
        return grantee;
    }

    public void setGrantee(AbstractGrantee grantee) {
        this.grantee = grantee;
    }

    @XmlElement(name = "Permission")
    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    @Override
    public int compareTo(Grant o) {
        int result = grantee.compareTo(o.getGrantee());
        if (result == 0) result = permission.compareTo(o.getPermission());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Grant grant = (Grant) o;

        if (grantee != null ? !grantee.equals(grant.grantee) : grant.grantee != null) return false;
        if (permission != grant.permission) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = grantee != null ? grantee.hashCode() : 0;
        result = 31 * result + (permission != null ? permission.hashCode() : 0);
        return result;
    }
}
