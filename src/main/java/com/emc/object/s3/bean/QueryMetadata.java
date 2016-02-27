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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlType(propOrder = {"type", "queryMetadataEntries"})
public class QueryMetadata {
    private QueryMetadataType type;
    private Map<String, String> mdMap = new HashMap<String, String>();

    @XmlElement(name = "type", namespace = "")
    public QueryMetadataType getType() {
        return type;
    }

    public void setType(QueryMetadataType type) {
        this.type = type;
    }

    @XmlTransient
    public Map<String, String> getMdMap() {
        return mdMap;
    }

    public void setMdMap(Map<String, String> mdMap) {
        this.mdMap = mdMap;
    }

    /**
     * @deprecated Use {@link #getMdMap()} instead.
     */
    @XmlElementWrapper(name = "mdMap", namespace = "")
    @XmlElement(name = "entry", namespace = "")
    public List<QueryMetadataEntry> getQueryMetadataEntries() {
        if (mdMap == null) return null;
        List<QueryMetadataEntry> queryMetadataEntries = new ArrayList<QueryMetadataEntry>();
        for (String key : mdMap.keySet()) {
            queryMetadataEntries.add(new QueryMetadataEntry(key, mdMap.get(key)));
        }
        return queryMetadataEntries;
    }

    /**
     * @deprecated Use {@link #setMdMap(Map)} instead.
     */
    public void setQueryMetadataEntries(List<QueryMetadataEntry> queryMetadataEntries) {
        mdMap = new HashMap<String, String>();
        for (QueryMetadataEntry entry : queryMetadataEntries) {
            mdMap.put(entry.getKey(), entry.getValue());
        }
    }

    public static class QueryMetadataEntry {
        private String key;
        private String value;

        public QueryMetadataEntry() {
        }

        public QueryMetadataEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @XmlElement(name = "key", namespace = "")
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @XmlElement(name = "value", namespace = "")
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
