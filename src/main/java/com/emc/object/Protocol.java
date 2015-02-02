/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.object;

public enum Protocol {
    HTTP("http"), HTTPS("https");

    private String name;

    private Protocol(String name) {
        this.name = name;
    }
}
