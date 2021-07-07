package com.emc.object.s3.bean;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

//Adaptor for AWS Datetime type TIMESTAMP used by Object Lock Retention.
public class DateTimeAdapter  extends XmlAdapter<String, Date> {
    private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final ZoneId zoneId_UTC = TimeZone.getTimeZone("UTC").toZoneId();

    @Override
    public String marshal(Date v) throws Exception {
        return DateTimeFormatter.ofPattern(dateFormat).format(v.toInstant().atZone(zoneId_UTC));
    }

    @Override
    public Date unmarshal(String v) throws Exception {
        return Date.from(ZonedDateTime.parse(v, DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId_UTC)).toInstant());
    }

}