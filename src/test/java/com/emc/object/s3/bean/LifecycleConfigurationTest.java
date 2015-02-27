/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
