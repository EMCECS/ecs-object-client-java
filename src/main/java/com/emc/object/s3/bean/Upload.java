/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object.s3.bean;

import java.util.Date;

public class Upload {
    private String key;
    private String uploadId;
    private CanonicalUser initiator;
    private CanonicalUser owner;
    private StorageClass storageClass;
    private Date initiated;
}
