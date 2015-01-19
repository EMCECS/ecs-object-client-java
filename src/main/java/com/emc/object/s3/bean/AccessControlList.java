package com.emc.object.s3.bean;

import com.emc.object.util.RestUtil;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.*;

@XmlRootElement(name = "AccessControlPolicy")
@XmlType(propOrder = {"owner", "grants"})
public class AccessControlList {
    private Owner owner;
    private Set<Grant> grants = new LinkedHashSet<>();

    @XmlElement(name = "Owner")
    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @XmlElementWrapper(name = "AccessControlList")
    @XmlElement(name = "Grant")
    public Set<Grant> getGrants() {
        return grants;
    }

    public void setGrants(Set<Grant> grants) {
        this.grants = grants;
    }

    public Map<String, List<Object>> toHeaders() {
        Map<String, List<Object>> headers = new HashMap<>();
        for (Grant grant : grants) {
            RestUtil.add(headers, grant.getPermission().getHeaderName(), grant.getGrantee().getHeaderValue());
        }
        return headers;
    }
}
