package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.S3Constants;
import com.emc.object.s3.bean.AccessControlList;
import com.emc.object.s3.bean.CannedAcl;
import com.emc.object.util.RestUtil;

import java.util.List;
import java.util.Map;

public class SetBucketAclRequest extends AbstractBucketRequest<SetBucketAclRequest> implements EntityRequest<AccessControlList> {
    private AccessControlList acl;
    private CannedAcl cannedAcl;

    @Override
    protected SetBucketAclRequest me() {
        return this;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public String getQuery() {
        return "acl";
    }

    @Override
    public Map<String, List<Object>> getHeaders() {
        Map<String, List<Object>> headers = super.getHeaders();
        if (cannedAcl != null) RestUtil.putSingle(headers, S3Constants.AMZ_ACL, cannedAcl.getHeaderValue());
        return headers;
    }

    @Override
    public AccessControlList getEntity() {
        return getAcl();
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    public CannedAcl getCannedAcl() {
        return cannedAcl;
    }

    public void setCannedAcl(CannedAcl cannedAcl) {
        this.cannedAcl = cannedAcl;
    }

    public SetBucketAclRequest withAcl(AccessControlList acl) {
        setAcl(acl);
        return this;
    }

    public SetBucketAclRequest withCannedAcl(CannedAcl cannedAcl) {
        setCannedAcl(cannedAcl);
        return this;
    }
}
