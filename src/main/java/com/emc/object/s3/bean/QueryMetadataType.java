package com.emc.object.s3.bean;

import jakarta.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum QueryMetadataType {
    SYSMD, USERMD
}
