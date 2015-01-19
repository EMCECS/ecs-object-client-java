package com.emc.object.s3.bean;

public enum Region {
    US(""),
    US_WEST_1("us-west-1"),
    US_WEST_2("us-west-2"),
    EU("eu-west-1"),
    EU_CENTRAL_1("us-west-1"),
    AP_SOUTHEAST_1("us-west-1"),
    AP_SOUTHEAST_2("us-west-1"),
    AP_NORTHEAST_1("us-west-1"),
    SA_EAST_1("us-west-1");

    public static Region fromConstraint(String constraint) {
        for (Region region : values()) {
            if (region.getConstraint().equals(constraint)) return region;
        }
        return null;
    }

    private String constraint;

    private Region(String constraint) {
        this.constraint = constraint;
    }

    public String getConstraint() {
        return constraint;
    }
}
