package com.emc.object;

public enum Method {
    HEAD(false), GET(false), PUT(true), POST(true), DELETE(false);

    private boolean requiresEntity;

    private Method(boolean requiresEntity) {
        this.requiresEntity = requiresEntity;
    }

    public boolean isRequiresEntity() {
        return requiresEntity;
    }
}
