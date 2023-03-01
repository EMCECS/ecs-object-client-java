package com.emc.object.s3.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * List of search metadata associated with a bucket.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "MetadataSearchList")
public class MetadataSearchList {

    private List<MetadataSearchKey> indexableKeys = new ArrayList<MetadataSearchKey>();
    private List<MetadataSearchKey> optionalAttributes = new ArrayList<MetadataSearchKey>();

    @JacksonXmlElementWrapper(localName = "IndexableKeys")
    @XmlElement(name = "Key")
    public List<MetadataSearchKey> getIndexableKeys() {
        return indexableKeys;
    }

    public void setIndexableKeys(List<MetadataSearchKey> indexableKeys) {
        this.indexableKeys = indexableKeys;
    }

    @JacksonXmlElementWrapper(localName = "OptionalAttributes")
    @XmlElement(name = "Attribute")
    public List<MetadataSearchKey> getOptionalAttributes() {
        return optionalAttributes;
    }

    public void setOptionalAttributes(List<MetadataSearchKey> optionalAttributes) {
        this.optionalAttributes = optionalAttributes;
    }
}
