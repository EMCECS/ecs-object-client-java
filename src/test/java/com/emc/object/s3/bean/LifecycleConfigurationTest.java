/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LifecycleConfigurationTest {
    @Test
    public void testMarshalling() throws Exception {
        JAXBContext context = JAXBContext.newInstance(LifecycleConfiguration.class, LifecycleRule.class);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<LifecycleConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                "<Rule>" +
                "<ID>Archive and then delete rule</ID>" +
                "<Prefix>projectdocs/</Prefix>" +
                "<Status>Enabled</Status>" +
                "<Expiration>" +
                "<Days>3650</Days>" +
                "</Expiration>" +
                "</Rule>" +
                "<Rule>" +
                "<Prefix>foo/</Prefix>" +
                "<Status>Disabled</Status>" +
                "<Expiration>" +
                "<Date>2050-01-01T00:00:00Z</Date>" +
                "</Expiration>" +
                "</Rule>" +
                "</LifecycleConfiguration>";

        List<LifecycleRule> rules = new ArrayList<LifecycleRule>();
        rules.add(new LifecycleRule("Archive and then delete rule", "projectdocs/", LifecycleRule.Status.Enabled, 3650));
        rules.add(new LifecycleRule(null, "foo/", LifecycleRule.Status.Disabled, new Date(2524608000000L)));

        LifecycleConfiguration object = new LifecycleConfiguration();
        object.setRules(rules);

        // unmarshall and compare to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        LifecycleConfiguration unmarshalledObject = (LifecycleConfiguration) unmarshaller.unmarshal(new StringReader(xml));
        for (LifecycleRule rule : object.getRules()) {
            Assert.assertTrue(unmarshalledObject.getRules().contains(rule));
        }
        for (LifecycleRule rule : unmarshalledObject.getRules()) {
            Assert.assertTrue(object.getRules().contains(rule));
        }

        // re-marshall and compare to string
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        Assert.assertEquals(xml, writer.toString());
    }
}
