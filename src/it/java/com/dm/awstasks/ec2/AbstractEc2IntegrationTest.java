package com.dm.awstasks.ec2;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;

import com.dm.awstasks.AbstractTest;
import com.xerox.amazonws.ec2.LaunchConfiguration;

public abstract class AbstractEc2IntegrationTest extends AbstractTest {

    protected static final Logger LOG = Logger.getLogger(AbstractEc2IntegrationTest.class);

    private static final String EC2_PROPERTIES_FILE = "/ec2.properties";
    private static final String ACCESS_KEY = "ec2.accessKey";
    private static final String ACCESS_KEY_SECRET = "ec2.accessSecret";
    private static final String PRIVATE_KEY_NAME = "ec2.privateKeyName";
    private static final String PRIVATE_KEY_FILE = "ec2.privateKeyFile";

    public static final String TEST_SECURITY_GROUP = "aws-tasks.test";

    protected static String _accessKeyId;
    protected static String _accessKeySecret;
    protected static String _privateKeyName;
    protected static String _privateKeyFile;

    @BeforeClass
    public static void readEc2Properties() throws IOException {
        InputStream inputStream = AbstractEc2IntegrationTest.class.getResourceAsStream(EC2_PROPERTIES_FILE);
        if (inputStream != null) {
            Properties properties = new Properties();
            properties.load(inputStream);
            _accessKeyId = properties.getProperty(ACCESS_KEY);
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

    protected static LaunchConfiguration createLaunchConfiguration(int instanceCount) {
        String imageId = "ami-5059be39";
        LaunchConfiguration launchConfiguration = new LaunchConfiguration(imageId, instanceCount, instanceCount);
        launchConfiguration.setKeyName(_privateKeyName);
        launchConfiguration.setSecurityGroup(Arrays.asList(TEST_SECURITY_GROUP, "default"));
        return launchConfiguration;
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
