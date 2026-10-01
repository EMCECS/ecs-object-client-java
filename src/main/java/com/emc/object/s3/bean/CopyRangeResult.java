package com.emc.object.s3.bean;

import com.emc.object.ObjectResponse;
import com.emc.object.util.RestUtil;

import jakarta.xml.bind.annotation.XmlTransient;

public class CopyRangeResult extends ObjectResponse {

    @XmlTransient
    public String getETag() {
        return firstHeader(RestUtil.HEADER_ETAG);
    }

}