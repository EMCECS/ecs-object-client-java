package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "DeleteResult")
public class DeleteObjectsResult {
    private List<AbstractDeleteResult> results;

    @XmlElementRef
    public List<AbstractDeleteResult> getResults() {
        return results;
    }

    public void setResults(List<AbstractDeleteResult> results) {
        this.results = results;
    }
}
