/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import com.emc.object.ObjectResponse;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3ObjectMetadata;
import com.emc.object.util.RestUtil;

import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;

public class PutObjectResult extends ObjectResponse {
    @XmlTransient
    public String getVersionId() {
        return firstHeader(S3Constants.AMZ_VERSION_ID);
    }

    @XmlTransient
    public Date getExpirationDate() {
        return S3ObjectMetadata.getExpirationDate(getHeaders());
    }

    @XmlTransient
    public String getExpirationRuleId() {
        return S3ObjectMetadata.getExpirationRuleId(getHeaders());
    }

    @XmlTransient
    public Long getAppendOffset() {
        String appendOffset = firstHeader(RestUtil.EMC_APPEND_OFFSET);
        return appendOffset == null ? null : Long.parseLong(appendOffset);
    }
}
