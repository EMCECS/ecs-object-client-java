package com.emc.object.s3.request;

import com.emc.object.EntityRequest;
import com.emc.object.Method;
import com.emc.object.s3.bean.DeleteObjects;
import com.emc.object.util.RestUtil;

import java.util.Arrays;

public class DeleteObjectsRequest extends AbstractBucketRequest implements EntityRequest<DeleteObjects> {
    private DeleteObjects deleteObjects;

    public DeleteObjectsRequest(String bucketName) {
        super(Method.POST, bucketName, "");
    }

    @Override
    public DeleteObjects getEntity() {
        return getDeleteObjects();
    }

    @Override
    public String getContentType() {
        return RestUtil.TYPE_APPLICATION_XML;
    }

    @Override
    public String getQuery() {
        return "delete";
    }

    public DeleteObjects getDeleteObjects() {
        return deleteObjects;
    }

    public void setDeleteObjects(DeleteObjects deleteObjects) {
        this.deleteObjects = deleteObjects;
    }

    public synchronized DeleteObjectsRequest withObjects(DeleteObjects.Object... objects) {
        if (deleteObjects == null)
            deleteObjects = new DeleteObjects();
        deleteObjects.setObjects(Arrays.asList(objects));
        return this;
    }

    public DeleteObjectsRequest withKeys(String... keys) {
        DeleteObjects.Object[] objects = new DeleteObjects.Object[keys.length];
        for (int i = 0; i < keys.length; i++) {
            objects[i] = new DeleteObjects.Object(keys[i]);
        }
        return withObjects(objects);
    }
}
