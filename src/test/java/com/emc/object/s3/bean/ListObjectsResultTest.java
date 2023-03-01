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
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ListObjectsResultTest {
    @Test
    public void testMarshalling() throws Exception {
        JAXBContext context = JAXBContext.newInstance(ListObjectsResult.class, CanonicalUser.class);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                "<IsTruncated>true</IsTruncated>" +
                "<Marker>key2</Marker>" +
                "<NextMarker>key3</NextMarker>" +
                "<Contents>" +
                "<ETag>&amp;quot;396fefef536d5ce46c7537ecf978a360&amp;quot;</ETag>" +
                "<Key>sourcekey</Key>" +
                "<LastModified>2050-01-01T00:00:00Z</LastModified>" +
                "<Owner>" +
                "<ID>ID12345</ID>" +
                "<DisplayName>Foo Bar</DisplayName>" +
                "</Owner>" +
                "<Size>217</Size>" +
                "<StorageClass>STANDARD</StorageClass>" +
                "</Contents>" +
                "<Contents>" +
                "<ETag>&amp;quot;396fefef536d5ce46c7537ecf978a360&amp;quot;</ETag>" +
                "<Key>key3</Key>" +
                "<LastModified>2050-01-01T00:00:00Z</LastModified>" +
                "<Owner>" +
                "<ID>ID12345</ID>" +
                "<DisplayName>Foo Bar</DisplayName>" +
                "</Owner>" +
                "<Size>217</Size>" +
                "<StorageClass>STANDARD</StorageClass>" +
                "</Contents>" +
                "<Contents>" +
                "<ETag>&amp;quot;396fefef536d5ce46c7537ecf978a360&amp;quot;</ETag>" +
                "<Key>key%20with%20spaces</Key>" +
                "<LastModified>2050-01-01T00:00:00Z</LastModified>" +
                "<Owner>" +
                "<ID>ID12345</ID>" +
                "<DisplayName>Foo Bar</DisplayName>" +
                "</Owner>" +
                "<Size>124</Size>" +
                "<StorageClass>STANDARD</StorageClass>" +
                "</Contents>" +
                "<Name>bucket</Name>" +
                "<Prefix>my</Prefix>" +
                "<Delimiter>/</Delimiter>" +
                "<MaxKeys>1000</MaxKeys>" +
                "<CommonPrefixes>" +
                "<Prefix>photos/</Prefix>" +
                "</CommonPrefixes>" +
                "<CommonPrefixes>" +
                "<Prefix>videos%20space/</Prefix>" +
                "</CommonPrefixes>" +
                "<EncodingType>url</EncodingType>" +
                "</ListBucketResult>";

        List<S3Object> objects = new ArrayList<>();

        CanonicalUser owner = new CanonicalUser("ID12345", "Foo Bar");

        S3Object object = new S3Object();
        object.setKey("sourcekey");
        object.setLastModified(new Date(2524608000000L));
        object.setETag("&quot;396fefef536d5ce46c7537ecf978a360&quot;");
        object.setSize(217L);
        object.setOwner(owner);
        object.setStorageClass(StorageClass.STANDARD);
        objects.add(object);

        object = new S3Object();
        object.setKey("key3");
        object.setLastModified(new Date(2524608000000L));
        object.setETag("&quot;396fefef536d5ce46c7537ecf978a360&quot;");
        object.setSize(217L);
        object.setOwner(owner);
        object.setStorageClass(StorageClass.STANDARD);
        objects.add(object);

        object = new S3Object();
        object.setKey("key with spaces");
        object.setLastModified(new Date(2524608000000L));
        object.setETag("&quot;396fefef536d5ce46c7537ecf978a360&quot;");
        object.setSize(124L);
        object.setOwner(owner);
        object.setStorageClass(StorageClass.STANDARD);
        objects.add(object);

        ListObjectsResult objectsResult = new ListObjectsResult();
        objectsResult.setBucketName("bucket");
        objectsResult.setPrefix("my");
        objectsResult.setMarker("key2");
        objectsResult.setNextMarker("key3");
        objectsResult.setMaxKeys(1000);
        objectsResult.setDelimiter("/");
        objectsResult.setTruncated(true);
        objectsResult.setObjects(objects);
        objectsResult.set_commonPrefixes(Arrays.asList(new CommonPrefix("photos/"), new CommonPrefix("videos space/")));

        // unmarshall and compare to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ListObjectsResult unmarshalledObject = (ListObjectsResult) unmarshaller.unmarshal(new StringReader(xml));
        Assertions.assertEquals(objectsResult.getBucketName(), unmarshalledObject.getBucketName());
        Assertions.assertEquals(objectsResult.getPrefix(), unmarshalledObject.getPrefix());
        Assertions.assertEquals(objectsResult.getMarker(), unmarshalledObject.getMarker());
        Assertions.assertEquals(objectsResult.getNextMarker(), unmarshalledObject.getNextMarker());
        Assertions.assertEquals(objectsResult.getMaxKeys(), unmarshalledObject.getMaxKeys());
        Assertions.assertEquals(objectsResult.getDelimiter(), unmarshalledObject.getDelimiter());
        Assertions.assertEquals(objectsResult.isTruncated(), unmarshalledObject.isTruncated());
        Assertions.assertEquals(objectsResult.getCommonPrefixes(), unmarshalledObject.getCommonPrefixes());
        Assertions.assertEquals(objectsResult.getObjects().size(), unmarshalledObject.getObjects().size());
        for (int i = 0; i < objectsResult.getObjects().size(); i++) {
            S3Object obj = objectsResult.getObjects().get(i);
            S3Object unobj = unmarshalledObject.getObjects().get(i);
            Assertions.assertEquals(obj.getClass(), unobj.getClass());
            Assertions.assertEquals(obj.getKey(), unobj.getKey());
            Assertions.assertEquals(obj.getOwner(), unobj.getOwner());
            Assertions.assertEquals(obj.getLastModified(), unobj.getLastModified());
            Assertions.assertEquals(obj.getETag(), unobj.getETag());
            Assertions.assertEquals(obj.getSize(), unobj.getSize());
            Assertions.assertEquals(obj.getStorageClass(), unobj.getStorageClass());
        }
    }
}
