package com.dm.awstasks;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractIntegrationTest extends AbstractTest {

    protected static final Logger LOG = Logger.getLogger(AbstractIntegrationTest.class);

    private static final String EC2_PROPERTIES_FILE = "/ec2.properties";
    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String ACCESS_KEY_SECRET = "accessKeySecret";
    private static final String PRIVATE_KEY_NAME = "privateKeyName";
    private static final String PRIVATE_KEY_FILE = "privateKeyFile";

    protected static String _accessKeyId;
    protected static String _accessKeySecret;
    protected static String _privateKeyName;
    protected static String _privateKeyFile;

    @BeforeClass
    public static void readEc2Properties() throws IOException {
        InputStream inputStream = AbstractIntegrationTest.class.getResourceAsStream(EC2_PROPERTIES_FILE);
        if (inputStream != null) {
            Properties properties = new Properties();
            properties.load(inputStream);
            _accessKeyId = properties.getProperty(ACCESS_KEY_ID);
            _accessKeySecret = properties.getProperty(ACCESS_KEY_SECRET);
            _privateKeyName = properties.getProperty(PRIVATE_KEY_NAME);
            _privateKeyFile = properties.getProperty(PRIVATE_KEY_FILE);
        }
        if (isEc2Configured()) {
            LOG.info("read ec2 properties from file - running integration tests");
        } else {
            LOG.info("could not read ec2 properties from file - failing integration tests");
        }
    }

    @Before
    public void checkConfiguration() {
        if (!isEc2Configured()) {
            fail("can't run integration test without a properly configured src/it/resources/ec2.properties");
        } else if (!new File(_privateKeyFile).exists()) {
            fail(String.format("private key file '%s' not exists", _privateKeyFile));
        }
    }

    private static boolean isEc2Configured() {
        return !nullOrEmpty(_accessKeyId, _accessKeySecret, _privateKeyName, _privateKeyFile);
    }

    private static boolean nullOrEmpty(String... strings) {
        boolean nullOrEmpty = false;
        for (String string : strings) {
            if (nullOrEmpty(string)) {
                nullOrEmpty = true;
            }
        }
        return nullOrEmpty;

    }

    private static boolean nullOrEmpty(String string) {
        return string == null || string.trim().length() == 0;
    }

}
