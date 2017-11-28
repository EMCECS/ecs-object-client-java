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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlRootElement(name = "BucketPolicy", namespace = "")
@XmlType(propOrder = {"version", "id", "statements"})
public class BucketPolicy {
    private String version;
    private String id;
    private List<BucketPolicyStatement> statements = new ArrayList<BucketPolicyStatement>();

    public BucketPolicy() {}

    public BucketPolicy(String version, String id) {
        this.version = version;
        this.id = id;
    }

    public BucketPolicy withStatements(List<BucketPolicyStatement> statement){
        setStatements(statement);
        return this;
    }

    public BucketPolicy withStatements(BucketPolicyStatement... statements){
        setStatements(Arrays.asList(statements));
        return this;
    }

    @XmlElement(name = "Version", namespace = "")
    public String getVersion(){ return version; }

    public void setVersion(String version) { this.version = version; }

    @XmlElement(name = "Id", namespace = "")
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    @XmlElement(name = "Statement", namespace = "")
    public List<BucketPolicyStatement> getStatements() { return statements; }

    public void setStatements(List<BucketPolicyStatement> statements) { this.statements = statements; }
}