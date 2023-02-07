package com.emc.object.s3.bean;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "Tagging")
public class ObjectTagging {

    private List<ObjectTag> tagSet;

    @XmlElementWrapper(name = "TagSet")
    @XmlElement(name = "Tag")
    public List<ObjectTag> getTagSet() {
        return tagSet;
    }

    public void setTagSet(List<ObjectTag> tagSet) {
        this.tagSet = tagSet;
    }

    public ObjectTagging withTagSet(List<ObjectTag> tagSet) {
        setTagSet(tagSet);
        return this;
    }

    public Map<String, String> toStringMap() {
        Map<String, String> map = new HashMap<>();
        for (ObjectTag tag: this.tagSet) {
            map.putIfAbsent(tag.getKey(), tag.getValue());
        }
        return map;
    }

}
