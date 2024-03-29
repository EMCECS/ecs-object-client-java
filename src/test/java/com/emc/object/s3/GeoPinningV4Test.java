package com.emc.object.s3;

import com.emc.object.ObjectConfig;
import com.emc.object.s3.jersey.S3JerseyClient;
import com.emc.rest.smart.Host;
import com.emc.rest.smart.ecs.Vdc;
import org.junit.Assume;

import java.util.*;

public class GeoPinningV4Test extends GeoPinningTest {
    private List<Vdc> vdcs;

    @Override
    protected S3Config createS3Config() throws Exception {
        S3Config s3Config = super.createS3Config().withUseV2Signer(false);
        Assume.assumeFalse(s3Config.isUseVHost());

        // just going to use the same VDC thrice for lack of a geo env.
        List<? extends Host> hosts = s3Config.getVdcs().get(0).getHosts();
        Vdc vdc1 = new Vdc("vdc1", new ArrayList<Host>(hosts));
        Vdc vdc2 = new Vdc("vdc2", new ArrayList<Host>(hosts));
        Vdc vdc3 = new Vdc("vdc3", new ArrayList<Host>(hosts));

        vdcs = Arrays.asList(vdc1, vdc2, vdc3);

        String proxyUri = s3Config.getPropAsString(ObjectConfig.PROPERTY_PROXY_URI);
        s3Config = new S3Config(s3Config.getProtocol(), vdc1, vdc2, vdc3).withPort(s3Config.getPort())
                .withIdentity(s3Config.getIdentity()).withSecretKey(s3Config.getSecretKey());
        if (proxyUri != null) s3Config.setProperty(ObjectConfig.PROPERTY_PROXY_URI, proxyUri);

        s3Config.setGeoPinningEnabled(true);
        return s3Config;
    }

    @Override
    protected S3Client createS3Client() throws Exception {
        S3Client client = new S3JerseyClient(createS3Config().withUseV2Signer(false));

        Thread.sleep(500); // wait for polling daemon to finish initial poll

        return client;
    }
}
