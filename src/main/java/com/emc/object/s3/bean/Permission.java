package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum Permission {
    READ, WRITE, READ_ACP, WRITE_ACP, FULL_CONTROL
}
