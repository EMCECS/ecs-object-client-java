package com.emc.object.s3.bean;

import org.junit.Assert;
import org.junit.Test;

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
        JAXBContext context = JAXBContext.newInstance(AccessControlList.class, Owner.class, Grant.class, CanonicalUser.class, Group.class, Permission.class);

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

        Set<Grant> grants = new TreeSet<>();
        grants.add(new Grant(new CanonicalUser("foo", "Foo"), Permission.FULL_CONTROL));
        grants.add(new Grant(new CanonicalUser("bar", "Bar"), Permission.READ));
        grants.add(new Grant(new CanonicalUser("baz", "Baz"), Permission.WRITE));
        grants.add(new Grant(new Group("gwar"), Permission.READ_ACP));
        grants.add(new Grant(new Group("rawg"), Permission.WRITE_ACP));

        AccessControlList object = new AccessControlList();
        object.setOwner(new Owner("owner", "Owner"));
        object.setGrants(grants);

        // unmarshall and compare to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        AccessControlList unmarshalledObject = (AccessControlList) unmarshaller.unmarshal(new StringReader(xml));
        Assert.assertEquals(object.getOwner(), unmarshalledObject.getOwner());
        for (Grant grant : object.getGrants()) {
            Assert.assertTrue(unmarshalledObject.getGrants().contains(grant));
        }
        for (Grant grant : unmarshalledObject.getGrants()) {
            Assert.assertTrue(object.getGrants().contains(grant));
        }

        // re-marshall and compare to string
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(object, writer);
        Assert.assertEquals(xml, writer.toString());
    }
}
