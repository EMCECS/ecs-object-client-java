package com.emc.object.s3.bean;

import com.emc.object.ObjectResponse;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3ObjectMetadata;

import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;

public class PutObjectResult extends ObjectResponse {
    @XmlTransient
    public String getVersionId() {
        return headerAsString(S3Constants.AMZ_VERSION_ID);
    }

    @XmlTransient
    public Date getExpirationDate() {
        return S3ObjectMetadata.getExpirationDate(getHeaders());
    }

    @XmlTransient
    public String getExpirationRuleId() {
        return S3ObjectMetadata.getExpirationRuleId(getHeaders());
    }
}
