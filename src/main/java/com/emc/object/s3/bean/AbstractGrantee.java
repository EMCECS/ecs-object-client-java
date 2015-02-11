/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;
public abstract class AbstractGrantee implements Comparable<AbstractGrantee> {
    @Override
    public int compareTo(AbstractGrantee o) {
        return getClass().getName().compareTo(o.getClass().getName());
    }

    public abstract String getHeaderValue();
}
