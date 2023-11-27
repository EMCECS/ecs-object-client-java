package com.emc.object.s3.jersey;

import javax.ws.rs.Priorities;

public class FilterPriorities {

    // providers used during request processing (using ascending manner)
    public static final int PRIORITY_NAMESPACE = 1000;
    public static final int PRIORITY_BUCKET = 1100;
    public static final int PRIORITY_GEOPINNING = 1200;
    // Priority of SmartFilter is 1400
    public static final int PRIORITY_CHECKSUM_REQUEST = 1600;
    public static final int PRIORITY_CODEC_REQUEST = 1800;
    public static final int PRIORITY_AUTHORIZATION = Priorities.AUTHORIZATION; //2000
    public static final int PRIORITY_FAULTINJECTION = 2400;

    // providers used during response processing (using descending manner)
    public static final int PRIORITY_CHECKSUM_RESPONSE = 4500;
    public static final int PRIORITY_CODEC_RESPONSE = 4000;
    public static final int PRIORITY_ERROR = 3600;

    private FilterPriorities() {}

}