/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
@XmlSchema(namespace = "http://s3.amazonaws.com/doc/2006-03-01/", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)
@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(value = com.emc.object.util.Iso8601Adapter.class, type = java.util.Date.class),
        @XmlJavaTypeAdapter(value = RegionAdapter.class, type = com.emc.object.s3.bean.Region.class)
}) package com.emc.object.s3.bean;

import com.emc.object.s3.RegionAdapter;

import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;