package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "Delete")
public class DeleteObjects {
    private Boolean quiet;
    private List<Object> objects;

    @XmlElement(name = "Quiet")
    public Boolean getQuiet() {
        return quiet;
    }

    public void setQuiet(Boolean quiet) {
        this.quiet = quiet;
    }

    @XmlElement(name = "Object")
    public List<Object> getObjects() {
        return objects;
    }

    public void setObjects(List<Object> objects) {
        this.objects = objects;
    }

    public static class Object {
        private String key;
        private String versionId;

        public Object(String key) {
            this.key = key;
        }

        public Object(String key, String versionId) {
            this.key = key;
            this.versionId = versionId;
        }

        @XmlElement(name = "Key")
        public String getKey() {
            return key;
        }

        @XmlElement(name = "VersionId")
        public String getVersionId() {
            return versionId;
        }

        public void setVersionId(String versionId) {
            this.versionId = versionId;
        }
    }
}
