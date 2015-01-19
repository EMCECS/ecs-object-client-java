package com.emc.object.s3.bean;

import com.emc.object.s3.S3Constants;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum Permission {
    READ(S3Constants.AMZ_GRANT_READ),
    WRITE(S3Constants.AMZ_GRANT_WRITE),
    READ_ACP(S3Constants.AMZ_GRANT_READ_ACP),
    WRITE_ACP(S3Constants.AMZ_GRANT_WRITE_ACP),
    FULL_CONTROL(S3Constants.AMZ_GRANT_FULL_CONTROL);

    private String headerName;

    private Permission(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }
}
