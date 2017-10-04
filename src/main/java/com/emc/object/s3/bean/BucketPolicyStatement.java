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
import javax.xml.bind.annotation.XmlEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BucketPolicyStatement {
    private String sId;
    private Effect effect;
    private List<String> principals = new ArrayList<String>();
    private List<String> notPrincipals;
    private List<BucketPolicyAction> actions = new ArrayList<BucketPolicyAction>();
    private List<BucketPolicyAction> notActions;
    private String resource;
    private String notResource;
    private String conditions;

    /*public BucketPolicyStatement(String sId, String resource, List<String> principals, Effect effect, List<String> actions) {
        this.sId = sId;
        this.resource = resource;
        this.principals = principals;
        this.effect = effect;
        this.actions = actions;
    }*/

    public BucketPolicyStatement withSid(String sId) {
        setSid(sId);
        return this;
    }

    public BucketPolicyStatement withResource(String resource) {
        setResource(resource);
        return this;
    }

    public BucketPolicyStatement withPrincipals(List<String> principals) {
        setPrincipals(principals);
        return this;
    }

    public BucketPolicyStatement withPrincipals(String... principals) {
        return withPrincipals(Arrays.asList(principals));
    }

    public BucketPolicyStatement withActions(List<BucketPolicyAction> actions) {
        setActions(actions);
        return this;
    }

    public BucketPolicyStatement withActions(BucketPolicyAction... actions) {
        return withActions(Arrays.asList(actions));
    }

    public BucketPolicyStatement withEffect(Effect effect) {
        setEffect(effect);
        return this;
    }

    @XmlElement(name = "Sid")
    public String getSid() { return sId; }

    public void setSid(String sId) { this.sId = sId; }

    @XmlElement(name = "Effect")
    public Effect getEffect() { return effect; }

    public void setEffect(Effect effect) { this.effect = effect; }

    @XmlElement(name = "Principal")
    public List<String> getPrincipals() { return principals; }

    public void setPrincipals(List<String> principals) { this.principals = principals; }

    @XmlElement(name = "NotPrincipal")
    public List<String> getNotPrincipals() { return notPrincipals; }

    public void setNotPrincipals(List<String> notPrincipals) { this.notPrincipals = notPrincipals; }

    @XmlElement(name = "Action")
    public List<BucketPolicyAction> getActions() { return actions; }

    public void setActions(List<BucketPolicyAction> actions) { this.actions = actions; }

    @XmlElement(name = "NotAction")
    public List<BucketPolicyAction> getNotActions() { return notActions; }

    public void setNotActions(List<BucketPolicyAction> notActions) { this.notActions = notActions; }

    @XmlElement(name = "Resource")
    public String getResource() { return resource; }

    public void setResource(String resource) { this.resource = resource; }

    @XmlElement(name = "NotResource")
    public String getNotResource() { return notResource; }

    public void setNotResource(String notResource) { this.notResource = notResource; }

    @XmlElement(name = "Condition")
    public String getConditions() { return conditions; }

    public void setCondition(String conditions) { this.conditions = conditions; }

    @XmlEnum
    public static enum Effect {
        Allow, Deny
    }
}
