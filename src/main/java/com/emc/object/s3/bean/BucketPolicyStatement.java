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

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.xml.bind.annotation.*;
import java.io.IOException;
import java.util.*;

@XmlType(propOrder = {"sid", "effect", "rawPrincipal", "actions", "resource", "conditions"})
public class BucketPolicyStatement {
    private String sid;
    private Effect effect;
    private String principal;
    private List<BucketPolicyAction> actions = new ArrayList<BucketPolicyAction>();
    private String resource;
    private Map<PolicyConditionOperator, PolicyConditionCriteria> conditions = new TreeMap<PolicyConditionOperator, PolicyConditionCriteria>();

    public BucketPolicyStatement() {}

    @XmlElement(name = "Sid")
    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    @XmlElement(name = "Effect")
    public Effect getEffect() { return effect; }

    public void setEffect(Effect effect) { this.effect = effect; }

    @XmlTransient
    public String getPrincipal() {
        if ("\"*\"".equals(principal)) return "*"; // backward-compatible for "*"
        return principal;
    }

    @XmlElement(name = "Principal")
    @JsonRawValue()
    @JsonDeserialize(using = RawDeserializer.class)
    public String getRawPrincipal() { return principal; }

    public void setPrincipal(String principal) {
        if ("*".equals(principal)) this.principal = "\"*\""; // backward-compatible for "*"
        else this.principal = principal;
    }

    /**
     * If you want to set the principal to something other than "*", you'll need to set a raw JSON value here.
     * I.e. <code>"{\"AWS\":[\"arn:ecs:iam::ns:user/my-user\",\"arn:ecs:iam::ns:user/other-user\"]}"</code>
     */
    public void setRawPrincipal(String principal) { this.principal = principal; }

    @XmlElement(name = "Action")
    public List<BucketPolicyAction> getActions() {
        return actions;
    }

    public void setActions(List<BucketPolicyAction> actions) {
        this.actions = actions;
    }

    @XmlElement(name = "Resource")
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @XmlElementWrapper(name = "Condition")
    public Map<PolicyConditionOperator, PolicyConditionCriteria> getConditions() {
        return conditions;
    }

    public void setConditions(Map<PolicyConditionOperator, PolicyConditionCriteria> conditions) {
        this.conditions = conditions;
    }

    public BucketPolicyStatement withSid(String sid) {
        setSid(sid);
        return this;
    }

    public BucketPolicyStatement withEffect(Effect effect) {
        setEffect(effect);
        return this;
    }

    public BucketPolicyStatement withPrincipal(String principal) {
        setPrincipal(principal);
        return this;
    }

    public BucketPolicyStatement withActions(List<BucketPolicyAction> actions) {
        setActions(actions);
        return this;
    }

    public BucketPolicyStatement withActions(BucketPolicyAction... actions) {
        setActions(Arrays.asList(actions));
        return this;
    }

    public BucketPolicyStatement withResource(String resource) {
        setResource(resource);
        return this;
    }

    public BucketPolicyStatement withConditions(Map<PolicyConditionOperator, PolicyConditionCriteria> conditions) {
        setConditions(conditions);
        return this;
    }

    public BucketPolicyStatement withCondition(PolicyConditionOperator operator, PolicyConditionCriteria criteria) {
        conditions.put(operator, criteria);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BucketPolicyStatement that = (BucketPolicyStatement) o;

        if (sid != null ? !sid.equals(that.sid) : that.sid != null) return false;
        if (effect != that.effect) return false;
        if (principal != null ? !principal.equals(that.principal) : that.principal != null) return false;
        if (actions != null ? !actions.equals(that.actions) : that.actions != null) return false;
        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        return conditions != null ? conditions.equals(that.conditions) : that.conditions == null;
    }

    @Override
    public int hashCode() {
        int result = sid != null ? sid.hashCode() : 0;
        result = 31 * result + (effect != null ? effect.hashCode() : 0);
        result = 31 * result + (principal != null ? principal.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (conditions != null ? conditions.hashCode() : 0);
        return result;
    }

    @XmlEnum
    public enum Effect {
        Allow, Deny
    }

    public static class RawDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return p.getCodec().readTree(p).toString();
        }
    }
}