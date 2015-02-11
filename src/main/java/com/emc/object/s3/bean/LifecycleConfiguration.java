/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "LifecycleConfiguration")
public class LifecycleConfiguration {
    private List<LifecycleRule> rules = new ArrayList<LifecycleRule>();

    @XmlElement(name = "Rule")
    public List<LifecycleRule> getRules() {
        return rules;
    }

    public void setRules(List<LifecycleRule> rules) {
        this.rules = rules;
    }
}
