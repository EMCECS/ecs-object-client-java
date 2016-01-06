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
package com.emc.object.s3;

import com.emc.object.s3.jersey.GeoPinningFilter;
import org.apache.commons.cli.*;

public class GeoPinCli {
    public static final String JAR_NAME = "geo-pin-cli-{version}.jar";
    public static final String VERSION = GeoPinCli.class.getPackage().getImplementationVersion();

    public static void main(String[] args) throws Exception {
        CommandLine line = null;
        try {
            System.out.println(versionLine());

            line = new DefaultParser().parse(options(), args);

            int vdcCount = Integer.parseInt(line.getOptionValue('v'));
            String bucketName = line.getOptionValue('b');
            String keyName = line.getOptionValue('k');
            if (keyName == null) keyName = "";

            int geoPinnedVdc = GeoPinningFilter.getGeoPinIndex(GeoPinningFilter.getGeoId(bucketName, keyName), vdcCount);

            geoPinnedVdc++; // print 1-based number, not 0-based

            System.out.println(String.format("VDC Count: %d", vdcCount));
            System.out.println(String.format("Bucket Name: %s", bucketName));
            System.out.println(String.format("Key Name: %s", keyName));
            System.out.println(String.format("** Geo-Pinned VDC: %d", geoPinnedVdc));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            help();
            if (line != null && line.hasOption("stacktrace")) e.printStackTrace();
            System.exit(1);
        }
    }

    public static Options options() {
        OptionGroup guid = new OptionGroup();
        guid.addOption(Option.builder("b").longOpt("bucket").desc("Name of the S3 bucket")
                .required().hasArg().argName("bucket-name").build());
        guid.addOption(Option.builder("k").longOpt("key").desc("Object key (name)")
                .hasArg().argName("object-key").build());
        guid.setRequired(true);
        Options options = new Options();
        options.addOptionGroup(guid);
        options.addOption(Option.builder("v").longOpt("vdc-count").desc("Total number of VDCs in the cloud")
                .required().hasArg().argName("vdc-count").build());
        options.addOption(Option.builder().longOpt("stacktrace").desc("Prints a detailed stacktrace for errors").build());
        return options;
    }

    public static String versionLine() {
        return GeoPinCli.class.getSimpleName() + (VERSION == null ? "" : " v" + VERSION);
    }

    public static void help() {
        System.out.println();
        System.out.println(GeoPinCli.class.getSimpleName() +
                " calculates which VDC a request will go to when geo-pinning is enabled");
        System.out.println();
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar " + JAR_NAME + ".jar", options(), true);
        System.out.println("* note that geo-pinning will only distribute among healthy, accessible VDCs");
        System.out.println("* note also that if geo-read-retry-failover is enabled and a read request fails,");
        System.out.println("  each retry will go to the next VDC in order");
        System.out.println();
    }
}
