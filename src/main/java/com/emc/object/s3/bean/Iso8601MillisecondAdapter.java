package com.emc.object.s3.bean;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.ZonedDateTime;
import java.util.Date;

import com.emc.object.util.RestUtil;

//DateTime Adaptor for AWS Object Lock Retention
public class Iso8601MillisecondAdapter extends XmlAdapter<String, Date> {

    @Override
    public String marshal(Date v) throws Exception {
        return RestUtil.iso8601MillisecondFormatter.format(v.toInstant());
    }

    @Override
    public Date unmarshal(String v) throws Exception {
        return Date.from(ZonedDateTime.parse(v, RestUtil.iso8601MillisecondFormatter).toInstant());
    }

}