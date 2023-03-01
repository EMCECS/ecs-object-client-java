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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@XmlType(propOrder = {"type", "mdMap"}, namespace = "")
public class QueryMetadata {
    private QueryMetadataType type;
    private Map<String, String> mdMap = new TreeMap<String, String>();

    @XmlElement(name = "type")
    public QueryMetadataType getType() {
        return type;
    }

    public void setType(QueryMetadataType type) {
        this.type = type;
    }

    @XmlJavaTypeAdapter(MapAdapter.class)
    @XmlElement(name = "mdMap")
    public Map<String, String> getMdMap() {
        return mdMap;
    }

    public void setMdMap(Map<String, String> mdMap) {
        this.mdMap = mdMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryMetadata metadata = (QueryMetadata) o;

        if (type != metadata.type) return false;
        return mdMap != null ? mdMap.equals(metadata.mdMap) : metadata.mdMap == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (mdMap != null ? mdMap.hashCode() : 0);
        return result;
    }

    public static class MapAdapter extends XmlAdapter<FlatMap, Map<String, String>> {
        @Override
        public Map<String, String> unmarshal(FlatMap v) throws Exception {
            Map<String, String> map = new TreeMap<String, String>();
            for (Entry entry : v.entry) {
                map.put(entry.key, entry.value);
            }
            return map;
        }

        @Override
        public FlatMap marshal(Map<String, String> v) throws Exception {
            FlatMap flatMap = new FlatMap();
            for (String key : v.keySet()) {
                flatMap.entry.add(new Entry(key, v.get(key)));
            }
            return flatMap;
        }
    }

    @XmlType(namespace = "")
    public static class FlatMap {
        public List<Entry> entry = new ArrayList<Entry>();
    }

    @XmlType(namespace = "")
    public static class Entry {
        public String key;
        public String value;

        public Entry() {
        }

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
