/*
 * Copyright (c) 2015-2016, EMC Corporation.
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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class QueryObjectResultTest {
    @Test
    public void testMarshalling() throws Exception {
        JAXBContext context = JAXBContext.newInstance(QueryObjectsResult.class);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<BucketQueryResult>" +
                "<Name>s3-metadata-search-test-arnetc-26388</Name>" +
                "<NextMarker>NO MORE PAGES</NextMarker>" +
                "<MaxKeys>1000</MaxKeys>" +
                "<ObjectMatches>" +
                "<object>" +
                "<objectName>object1</objectName>" +
                "<objectId>5c5e56696ee4413109b37a4e3e602032c3642378e410b90c4f19e4b08fb1ec16</objectId>" +
                "<versionId>0</versionId>" +
                "<queryMds><type>SYSMD</type>" +
                "<mdMap>" +
                "<entry><key>ctype</key><value>application/octet-stream</value></entry>" +
                "<entry><key>size</key><value>0</value></entry>" +
                "</mdMap>" +
                "</queryMds>" +
                "<queryMds><type>USERMD</type>" +
                "<mdMap>" +
                "<entry><key>x-amz-meta-datetime1</key><value>2015-01-01T00:00:00Z</value></entry>" +
                "<entry><key>x-amz-meta-decimal1</key><value>3.14159</value></entry>" +
                "<entry><key>x-amz-meta-integer1</key><value>42</value></entry>" +
                "<entry><key>x-amz-meta-string1</key><value>test</value></entry>" +
                "</mdMap>" +
                "</queryMds>" +
                "</object>" +
                "</ObjectMatches>" +
                "<CommonPrefixMatches>" +
                "<PrefixGroups>prefix/</PrefixGroups>" +
                "</CommonPrefixMatches>" +
                "</BucketQueryResult>";

        QueryObjectsResult result = new QueryObjectsResult();
        result.setBucketName("s3-metadata-search-test-arnetc-26388");
        result.setNextMarker("NO MORE PAGES");
        result.setMaxKeys(1000);

        List<QueryObject> objects = new ArrayList<QueryObject>();

        QueryObject object = new QueryObject();
        object.setObjectName("object1");
        object.setObjectId("5c5e56696ee4413109b37a4e3e602032c3642378e410b90c4f19e4b08fb1ec16");
        object.setVersionId("0");

        List<QueryMetadata> queryMds = new ArrayList<QueryMetadata>();

        QueryMetadata metadata = new QueryMetadata();
        metadata.setType(QueryMetadataType.SYSMD);
        Map<String, String> mdMap = new HashMap<String, String>();
        mdMap.put("ctype", "application/octet-stream");
        mdMap.put("size", "0");
        metadata.setMdMap(mdMap);
        queryMds.add(metadata);

        metadata = new QueryMetadata();
        metadata.setType(QueryMetadataType.USERMD);
        mdMap = new TreeMap<String, String>();
        mdMap.put("x-amz-meta-datetime1", "2015-01-01T00:00:00Z");
        mdMap.put("x-amz-meta-decimal1", "3.14159");
        mdMap.put("x-amz-meta-integer1", "42");
        mdMap.put("x-amz-meta-string1", "test");
        metadata.setMdMap(mdMap);
        queryMds.add(metadata);

        object.setQueryMds(queryMds);
        objects.add(object);

        result.setObjects(objects);

        List<String> prefixGroups = Arrays.asList("prefix/");
        result.setPrefixGroups(prefixGroups);

        // unmarshall and compare to object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        QueryObjectsResult unmarshalledObject = (QueryObjectsResult) unmarshaller.unmarshal(new StringReader(xml));

        Assert.assertEquals(result.getBucketName(), unmarshalledObject.getBucketName());
        Assert.assertEquals(result.getNextMarker(), unmarshalledObject.getNextMarker());
        Assert.assertEquals(result.getMaxKeys(), unmarshalledObject.getMaxKeys());

        for (QueryObject o : result.getObjects()) {
            Assert.assertTrue(unmarshalledObject.getObjects().contains(o));
        }
        for (QueryObject o : unmarshalledObject.getObjects()) {
            Assert.assertTrue(result.getObjects().contains(o));
        }

        // re-marshall and compare to string
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(result, writer);
        Assert.assertEquals(xml, writer.toString());
    }
}
