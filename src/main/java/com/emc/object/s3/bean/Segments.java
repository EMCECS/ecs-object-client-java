package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "Segments")
public class Segments {

    private List<Segment> segmentEntries;

    @XmlElement(name = "Segment")
    public List<Segment> getSegmentEntries() {
        return segmentEntries;
    }

    public void setSegmentEntries(List<Segment> segmentEntries) {
        this.segmentEntries = segmentEntries;
    }

    public Segments withSegmentEntries(List<Segment> segmentEntries) {
        setSegmentEntries(segmentEntries);
        return this;
    }
}
