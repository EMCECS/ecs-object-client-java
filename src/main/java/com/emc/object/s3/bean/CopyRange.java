package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *  Container for the multi copy range request
 */
@XmlRootElement(name = "CopyRangeRequest")
public class CopyRange {

    /**
     * This element defines the content type of target object
     * @valid none
     */
    private String contentType;

    /**
     * This element defines segments the target object will copy from
     * @valid none
     */
    private Segments segments;

    @XmlElement(name = "ContentType")
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @XmlElement(name = "Segments")
    public Segments getSegments() {
        return segments;
    }

    public void setSegments(Segments segments) {
        this.segments = segments;
    }

    public CopyRange withContentType(String contentType) {
        setContentType(contentType);
        return this;
    }

    public CopyRange withSegments(Segments segments) {
        setSegments(segments);
        return this;
    }

}
