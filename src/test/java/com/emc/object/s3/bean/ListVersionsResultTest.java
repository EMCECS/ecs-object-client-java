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
        Assert.assertEquals(object.isTruncated(), unmarshalledObject.isTruncated());
        Assert.assertEquals(object.getCommonPrefixes(), unmarshalledObject.getCommonPrefixes());
        Assert.assertEquals(object.getVersions().size(), unmarshalledObject.getVersions().size());
        for (int i = 0; i < object.getVersions().size(); i++) {
            AbstractVersion ver = object.getVersions().get(i);
            AbstractVersion unver = unmarshalledObject.getVersions().get(i);
            Assert.assertEquals(ver.getClass(), unver.getClass());
            Assert.assertEquals(ver.getKey(), unver.getKey());
            Assert.assertEquals(ver.getOwner(), unver.getOwner());
            Assert.assertEquals(ver.getLastModified(), unver.getLastModified());
            Assert.assertEquals(ver.isLatest(), unver.isLatest());
            Assert.assertEquals(ver.getVersionId(), unver.getVersionId());
            if (ver instanceof Version) {
                Assert.assertEquals(((Version) ver).geteTag(), ((Version) unver).geteTag());
                Assert.assertEquals(((Version) ver).getSize(), ((Version) unver).getSize());
                Assert.assertEquals(((Version) ver).getStorageClass(), ((Version) unver).getStorageClass());
            }
        }
    }
}
