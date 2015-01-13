package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum CorsMethod {
    GET, PUT, HEAD, POST, DELETE
}
