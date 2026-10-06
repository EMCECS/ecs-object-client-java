package com.emc.object.s3.bean;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Describes a metadata search key associated with a bucket.
 */
@XmlType(propOrder = {"name", "datatype"})
public class MetadataSearchKey {
    private String name;
    private MetadataSearchDatatype datatype;

    public MetadataSearchKey() {}

    public MetadataSearchKey(String name, MetadataSearchDatatype datatype) {
        this.name = name;
        this.datatype = datatype;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "Datatype")
    public MetadataSearchDatatype getDatatype() {
        return datatype;
    }

    public void setDatatype(MetadataSearchDatatype datatype) { this.datatype = datatype; }
}
