package com.dm.awstasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;

public abstract class AbstractIntegrationTest {

    protected static final Logger LOG = Logger.getLogger(AbstractIntegrationTest.class);

    private static final String EC2_PROPERTIES_FILE = "/ec2.properties";
    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String ACCESS_KEY_SECRET = "accessKeySecret";

    protected static String _accessKeyId;
    protected static String _accessKeySecret;

    @BeforeClass
    public static void readEc2Properties() throws IOException {
        InputStream inputStream = AbstractIntegrationTest.class.getResourceAsStream(EC2_PROPERTIES_FILE);
        Properties properties = new Properties();
        properties.load(inputStream);
        _accessKeyId = properties.getProperty(ACCESS_KEY_ID);
        _accessKeySecret = properties.getProperty(ACCESS_KEY_SECRET);
        if (isEc2Configured()) {
            LOG.info("read ec2 properties from file - running integration tests");
        } else {
            LOG.info("could not read ec2 properties from file - skipping integration tests");
        }
    }

    protected static boolean isEc2Configured() {
        return !nullOrEmpty(_accessKeyId) && !nullOrEmpty(_accessKeySecret);
    }

    private static boolean nullOrEmpty(String string) {
        return string == null || string.trim().length() == 0;
    }
}
