package com.emc.object.s3.bean;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.ZonedDateTime;
import java.util.Date;
import com.emc.object.util.RestUtil;

//Adaptor for AWS Datetime type TIMESTAMP used by Object Lock Retention.
public class DateTimeAdapter  extends XmlAdapter<String, Date> {

    @Override
    public String marshal(Date v) throws Exception {
        return RestUtil.awsTimestampFormatter.format(v.toInstant());
    }

    @Override
    public Date unmarshal(String v) throws Exception {
        return Date.from(ZonedDateTime.parse(v, RestUtil.awsTimestampFormatter).toInstant());
    }

}