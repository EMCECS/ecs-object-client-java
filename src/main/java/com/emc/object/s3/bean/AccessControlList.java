/*
 * Copyright (c) 2015, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3.bean;

import com.emc.object.util.RestUtil;

import javax.xml.bind.annotation.*;
import java.util.*;

@XmlRootElement(name = "AccessControlPolicy")
@XmlType(propOrder = {"owner", "grants"})
@XmlSeeAlso({CanonicalUser.class, Group.class})
public class AccessControlList {
    private CanonicalUser owner;
    private Set<Grant> grants = new LinkedHashSet<Grant>();

    @XmlElement(name = "Owner")
    public CanonicalUser getOwner() {
        return owner;
    }

    public void setOwner(CanonicalUser owner) {
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

    public void addGrants(Grant... grants) {
        this.grants.addAll(Arrays.asList(grants));
    }

    public Map<String, List<Object>> toHeaders() {
        Map<String, List<Object>> headers = new HashMap<String, List<Object>>();
        for (Grant grant : grants) {
            RestUtil.add(headers, grant.getPermission().getHeaderName(), grant.getGrantee().getHeaderValue());
        }
        return headers;
    }
}
