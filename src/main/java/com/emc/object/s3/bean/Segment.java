package com.emc.object.s3.bean;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Segment")
public class Segment {

    /**
     * This element defines the path of source object
     *
     * @valid none
     */
    @XmlElement(name = "Path")// todo required = true
    private String path;

    /**
     * This element defines the etag of source object
     *
     * @valid none
     */
    @JacksonXmlProperty(localName = "Etag")
    private String etag;

    /**
     * This element defines the range copy from source object
     *
     * @valid none
     */
    @JacksonXmlProperty(localName = "Range")
    private String range;

    /**
     * algorithm object was encrypted
     */
    @JacksonXmlProperty(localName = "SseCAlg")
    private String sse_c_alg;

    /**
     * base64-encoded encryption key for the object
     */
    @JacksonXmlProperty(localName = "SseCKey")
    private String sse_c_key;

    /**
     * base64-encoded 128-bit MD5 digest of the encryption key
     */
    @JacksonXmlProperty(localName = "SseCKeyMD5")
    private String sse_c_key_md5;

    public Segment() {
    }

    public Segment(String path, String etag, String range, String sse_c_alg, String sse_c_key, String sse_c_key_md5) {
        this.path = path;
        this.etag = etag;
        this.range = range;
        this.sse_c_alg = sse_c_alg;
        this.sse_c_key = sse_c_key;
        this.sse_c_key_md5 = sse_c_key_md5;
    }
}
