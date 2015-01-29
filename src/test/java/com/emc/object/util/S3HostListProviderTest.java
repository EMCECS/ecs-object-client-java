package com.emc.object.util;

import com.emc.object.s3.S3HostListProvider;
import com.emc.rest.smart.SmartConfig;
import com.emc.util.TestConfig;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class S3HostListProviderTest {
    @Test
    public void testS3HostListProvider() throws Exception {
        Properties properties = TestConfig.getProperties();

        URI serverURI = new URI(TestConfig.getPropertyNotEmpty(properties, TestProperties.S3_ENDPOINT));
        String user = TestConfig.getPropertyNotEmpty(properties, TestProperties.S3_ACCESS_KEY);
        String secret = TestConfig.getPropertyNotEmpty(properties, TestProperties.S3_SECRET_KEY);

        SmartConfig smartConfig = new SmartConfig(Arrays.asList(serverURI.getHost()));

        Client client = JerseyClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()));

        S3HostListProvider hostListProvider = new S3HostListProvider(client, smartConfig.getLoadBalancer(), user, secret);
        hostListProvider.setProtocol(serverURI.getScheme());
        hostListProvider.setPort(serverURI.getPort());

        List<String> hostList = hostListProvider.getHostList();

        Assert.assertTrue("server list is empty", hostList.size() > 0);
    }
}
