/*
 * Copyright 2014 EMC Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.emc.object.s3.bean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "ListDataNode", namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
public class ListDataNode {
    private List<String> dataNodes;
    private String versionInfo;

    @XmlElements(@XmlElement(name = "DataNodes", namespace = "http://s3.amazonaws.com/doc/2006-03-01/"))
    public List<String> getDataNodes() {
        return dataNodes;
    }

    public void setDataNodes(List<String> dataNodes) {
        this.dataNodes = dataNodes;
    }

    @XmlElement(name = "VersionInfo", namespace = "http://s3.amazonaws.com/doc/2006-03-01/")
    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }
}
