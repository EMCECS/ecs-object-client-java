package com.emc.object.s3.bean;

public enum CannedAcl {
    Private("private"),
    PublicRead("public-read"),
    PublicReadWrite("public-read-write"),
    AuthenticatedRead("authenticated-read"),
    LogDeliveryWrite("log-delivery-write"),
    BucketOwnerRead("bucket-owner-read"),
    BucketOwnerFullControl("bucket-owner-full-control");

    public static CannedAcl fromHeader(String header) {
        for (CannedAcl cannedAcl : values()) {
            if (cannedAcl.equals(header)) return cannedAcl;
        }
        return null;
    }

    private String header;

    private CannedAcl(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }
}
