/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ListVersionsResultTest {
    @Test
    public void testMarshalling() throws Exception {
        JAXBContext context = JAXBContext.newInstance(ListVersionsResult.class, CanonicalUser.class, Version.class, DeleteMarker.class);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<ListVersionsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                "<Name>bucket</Name><" +
                "Prefix>my</Prefix>" +
                "<KeyMarker>key2</KeyMarker>" +
                "<VersionIdMarker>t46ZenlYTZBnj</VersionIdMarker>" +
                "<NextKeyMarker>key3</NextKeyMarker>" +
                "<NextVersionIdMarker>d-d309mfjFrUmoQ0DBsVqmcMV15OI.</NextVersionIdMarker>" +
                "<MaxKeys>1000</MaxKeys>" +
                "<Delimiter>/</Delimiter>" +
                "<IsTruncated>true</IsTruncated>" +
                "<DeleteMarker>" +
                "<Key>sourcekey</Key>" +
                "<VersionId>qDhprLU80sAlCFLu2DWgXAEDgKzWarn-HS_JU0TvYqs.</VersionId>" +
                "<IsLatest>true</IsLatest>" +
                "<LastModified>2050-01-01T00:00:00Z</LastModified>" +
                "<Owner>" +
                "<ID>ID12345</ID>" +
                "<DisplayName>Foo Bar</DisplayName>" +
                "</Owner>" +
                "</DeleteMarker>" +
                "<Version>" +
                "<Key>sourcekey</Key>" +
                "<VersionId>wxxQ7ezLaL5JN2Sislq66Syxxo0k7uHTUpb9qiiMxNg.</VersionId>" +
                "<IsLatest>false</IsLatest>" +
                "<LastModified>2050-01-01T00:00:00Z</LastModified>" +
                "<ETag>&amp;quot;396fefef536d5ce46c7537ecf978a360&amp;quot;</ETag>" +
                "<Size>217</Size>" +
                "<Owner>" +
                "<ID>ID12345</ID>" +
                "<DisplayName>Foo Bar</DisplayName>" +
                "</Owner>" +
                "<StorageClass>STANDARD</StorageClass>" +
                "</Version>" +
                "<Version>" +
                "<Key>key3</Key>" +
                "<VersionId>d-d309mfjFri40QYukDozqBt3UmoQ0DBsVqmcMV15OI.</VersionId>" +
                "<IsLatest>false</IsLatest>" +
                "<LastModified>2050-01-01T00:00:00Z</LastModified>" +
                "<ETag>&amp;quot;396fefef536d5ce46c7537ecf978a360&amp;quot;</ETag>" +
                "<Size>217</Size>" +
                "<Owner>" +
                "<ID>ID12345</ID>" +
                "<DisplayName>Foo Bar</DisplayName>" +
                "</Owner>" +
                "<StorageClass>STANDARD</StorageClass>" +
                "</Version>" +
                "<CommonPrefixes>" +
                "<Prefix>photos/</Prefix>" +
                "</CommonPrefixes>" +
                "<CommonPrefixes>" +
                "<Prefix>videos/</Prefix>" +
                "</CommonPrefixes>" +
                "</ListVersionsResult>";

        List<AbstractVersion> versions = new ArrayList<AbstractVersion>();

        CanonicalUser owner = new CanonicalUser("ID12345", "Foo Bar");

        AbstractVersion version = new DeleteMarker();
        version.setKey("sourcekey");
        version.setVersionId("qDhprLU80sAlCFLu2DWgXAEDgKzWarn-HS_JU0TvYqs.");
        version.setLatest(true);
        version.setLastModified(new Date(2524608000000L));
        version.setOwner(owner);
        versions.add(version);

        version = new Version();
        version.setKey("sourcekey");
        version.setVersionId("wxxQ7ezLaL5JN2Sislq66Syxxo0k7uHTUpb9qiiMxNg.");
        version.setLatest(false);
        version.setLastModified(new Date(2524608000000L));
        ((Version) version).seteTag("&quot;396fefef536d5ce46c7537ecf978a360&quot;");
        ((Version) version).setSize(217L);
        version.setOwner(owner);
        ((Version) version).setStorageClass(StorageClass.STANDARD);
        versions.add(version);

        version = new Version();
        version.setKey("key3");
        version.setVersionId("d-d309mfjFri40QYukDozqBt3UmoQ0DBsVqmcMV15OI.");
        version.setLatest(false);
        version.setLastModified(new Date(2524608000000L));
        ((Version) version).seteTag("&quot;396fefef536d5ce46c7537ecf978a360&quot;");
        ((Version) version).setSize(217L);
        version.setOwner(owner);
        ((Version) version).setStorageClass(StorageClass.STANDARD);
        versions.add(version);

        ListVersionsResult object = new ListVersionsResult();
        object.setBucketName("bucket");
        object.setPrefix("my");
        object.setKeyMarker("key2");
        object.setVersionIdMarker("t46ZenlYTZBnj");
        object.setNextKeyMarker("key3");
        object.setNextVersionIdMarker("d-d309mfjFrUmoQ0DBsVqmcMV15OI.");
        object.setMaxKeys(1000);
        object.setDelimiter("/");
        object.setTruncated(true);
        object.setVersions(versions);
        object.set_commonPrefixes(Arrays.asList(new CommonPrefix("photos/"), new CommonPrefix("videos/")));

        // unmarshall and compare to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ListVersionsResult unmarshalledObject = (ListVersionsResult) unmarshaller.unmarshal(new StringReader(xml));
        Assert.assertEquals(object.getBucketName(), unmarshalledObject.getBucketName());
        Assert.assertEquals(object.getPrefix(), unmarshalledObject.getPrefix());
        Assert.assertEquals(object.getKeyMarker(), unmarshalledObject.getKeyMarker());
        Assert.assertEquals(object.getVersionIdMarker(), unmarshalledObject.getVersionIdMarker());
        Assert.assertEquals(object.getNextKeyMarker(), unmarshalledObject.getNextKeyMarker());
        Assert.assertEquals(object.getNextVersionIdMarker(), unmarshalledObject.getNextVersionIdMarker());
        Assert.assertEquals(object.getMaxKeys(), unmarshalledObject.getMaxKeys());
        Assert.assertEquals(object.getDelimiter(), unmarshalledObject.getDelimiter());
        Assert.assertEquals(object.getTruncated(), unmarshalledObject.getTruncated());
        Assert.assertEquals(object.getCommonPrefixes(), unmarshalledObject.getCommonPrefixes());
        Assert.assertEquals(object.getVersions().size(), unmarshalledObject.getVersions().size());
        for (int i = 0; i < object.getVersions().size(); i++) {
            AbstractVersion ver = object.getVersions().get(i);
            AbstractVersion unver = unmarshalledObject.getVersions().get(i);
            Assert.assertEquals(ver.getClass(), unver.getClass());
            Assert.assertEquals(ver.getKey(), unver.getKey());
            Assert.assertEquals(ver.getOwner(), unver.getOwner());
            Assert.assertEquals(ver.getLastModified(), unver.getLastModified());
            Assert.assertEquals(ver.getLatest(), unver.getLatest());
            Assert.assertEquals(ver.getVersionId(), unver.getVersionId());
            if (ver instanceof Version) {
                Assert.assertEquals(((Version) ver).geteTag(), ((Version) unver).geteTag());
                Assert.assertEquals(((Version) ver).getSize(), ((Version) unver).getSize());
                Assert.assertEquals(((Version) ver).getStorageClass(), ((Version) unver).getStorageClass());
            }
        }
    }
}
