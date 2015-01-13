package com.emc.object.s3.bean;

public abstract class Grantee implements Comparable<Grantee> {
    @Override
    public int compareTo(Grantee o) {
        return getClass().getName().compareTo(o.getClass().getName());
    }
}
