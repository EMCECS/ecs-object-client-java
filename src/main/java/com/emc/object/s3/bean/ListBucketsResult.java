package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListAllMyBucketsResult")
@XmlType(propOrder = {"owner", "buckets"})
public class ListBucketsResult {
    private CanonicalUser owner;
    private List<Bucket> buckets = new ArrayList<>();

    @XmlElement(name = "Owner")
    public CanonicalUser getOwner() {
        return owner;
    }

    public void setOwner(CanonicalUser owner) {
        this.owner = owner;
    }

    @XmlElementWrapper(name = "Buckets")
    @XmlElement(name = "Bucket")
    public List<Bucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<Bucket> buckets) {
        this.buckets = buckets;
    }
}
