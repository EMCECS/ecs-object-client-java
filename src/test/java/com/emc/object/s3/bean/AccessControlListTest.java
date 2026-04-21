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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;
import java.util.TreeSet;

public class AccessControlListTest {
    @Test
    public void testMarshall() throws Exception {
        JAXBContext context = JAXBContext.newInstance(AccessControlList.class, Grant.class, CanonicalUser.class, Group.class, Permission.class);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                "<Owner>" +
                "<ID>owner</ID>" +
                "<DisplayName>Owner</DisplayName>" +
                "</Owner>" +
                "<AccessControlList>" +
                "<Grant>" +
                "<Grantee xsi:type=\"CanonicalUser\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<ID>bar</ID>" +
                "<DisplayName>Bar</DisplayName>" +
                "</Grantee>" +
                "<Permission>READ</Permission>" +
                "</Grant>" +
                "<Grant>" +
                "<Grantee xsi:type=\"CanonicalUser\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<ID>baz</ID>" +
                "<DisplayName>Baz</DisplayName>" +
                "</Grantee>" +
                "<Permission>WRITE</Permission>" +
                "</Grant>" +
                "<Grant>" +
                "<Grantee xsi:type=\"CanonicalUser\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<ID>foo</ID>" +
                "<DisplayName>Foo</DisplayName>" +
                "</Grantee>" +
                "<Permission>FULL_CONTROL</Permission>" +
                "</Grant>" +
                "<Grant>" +
                "<Grantee xsi:type=\"Group\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<URI>gwar</URI>" +
                "</Grantee>" +
                "<Permission>READ_ACP</Permission>" +
                "</Grant>" +
                "<Grant>" +
                "<Grantee xsi:type=\"Group\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<URI>rawg</URI>" +
                "</Grantee>" +
                "<Permission>WRITE_ACP</Permission>" +
                "</Grant>" +
                "</AccessControlList>" +
                "</AccessControlPolicy>";

        Set<Grant> grants = new TreeSet<Grant>();
        grants.add(new Grant(new CanonicalUser("foo", "Foo"), Permission.FULL_CONTROL));
        grants.add(new Grant(new CanonicalUser("bar", "Bar"), Permission.READ));
        grants.add(new Grant(new CanonicalUser("baz", "Baz"), Permission.WRITE));
        grants.add(new Grant(new Group("gwar"), Permission.READ_ACP));
        grants.add(new Grant(new Group("rawg"), Permission.WRITE_ACP));

        AccessControlList object = new AccessControlList();
        object.setOwner(new CanonicalUser("owner", "Owner"));
        object.setGrants(grants);

        // unmarshall and compare to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        AccessControlList unmarshalledObject = (AccessControlList) unmarshaller.unmarshal(new StringReader(xml));
        Assertions.assertEquals(object.getOwner(), unmarshalledObject.getOwner());
        for (Grant grant : object.getGrants()) {
            Assertions.assertTrue(unmarshalledObject.getGrants().contains(grant));
        }
        for (Grant grant : unmarshalledObject.getGrants()) {
            Assertions.assertTrue(object.getGrants().contains(grant));
        }

        // re-marshall and compare to string
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        Assertions.assertEquals(xml, writer.toString());
    }
}
