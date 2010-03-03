/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datameer.awstasks.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.aws.emr.EmrSettings;

/**
 * Configuration class that can be used to read all ec2 access information from an properties file.
 */
public class Ec2Configuration {

    public static final String EC2_PROPERTIES_FILE = "/ec2.properties";
    private static final String ACCESS_KEY = "ec2.accessKey";
    private static final String ACCESS_KEY_SECRET = "ec2.accessSecret";
    private static final String PRIVATE_KEY_NAME = "ec2.privateKeyName";
    private static final String PRIVATE_KEY_FILE = "ec2.privateKeyFile";

    private Properties _properties;
    protected String _accessKeyId;
    protected String _accessKeySecret;
    protected String _privateKeyName;
    protected String _privateKeyFile;

    /**
     * Reads a ec2.properties from classpath. The path is {@link #EC2_PROPERTIES_FILE}.
     */
    public Ec2Configuration() throws IOException {
        InputStream inputStream = Ec2Configuration.class.getResourceAsStream(EC2_PROPERTIES_FILE);
        if (inputStream == null) {
            throw new IOException(EC2_PROPERTIES_FILE + " not found in classpath");
        }
        _properties = new Properties();
        _properties.load(inputStream);
        _accessKeyId = _properties.getProperty(ACCESS_KEY);
        _accessKeySecret = _properties.getProperty(ACCESS_KEY_SECRET);
        _privateKeyName = _properties.getProperty(PRIVATE_KEY_NAME);
        _privateKeyFile = _properties.getProperty(PRIVATE_KEY_FILE);
    }

    public boolean isEc2Configured() {
        return !nullOrEmpty(_accessKeyId, _accessKeySecret, _privateKeyName, _privateKeyFile);
    }

    public String getAccessKey() {
        return _accessKeyId;
    }

    public String getAccessSecret() {
        return _accessKeySecret;
    }

    public String getPrivateKeyName() {
        return _privateKeyName;
    }

    public String getPrivateKeyFile() {
        return _privateKeyFile;
    }

    public String getProperty(String name) {
        String property = _properties.getProperty(name);
        if (property == null) {
            throw new IllegalArgumentException("no property with name '" + name + "' configured");
        }
        return property;
    }

    public LaunchConfiguration createLaunchConfiguration(String ami, String group, int instanceCount) {
        LaunchConfiguration launchConfiguration = new LaunchConfiguration(ami, instanceCount, instanceCount);
        launchConfiguration.setKeyName(getPrivateKeyName());
        launchConfiguration.setSecurityGroup(Arrays.asList("default", group));
        return launchConfiguration;
    }

    public Jec2 createJEc2() {
        return new Jec2(_accessKeyId, _accessKeySecret);
    }

    public S3Service createS3Service() throws S3ServiceException {
        AWSCredentials awsCredentials = new AWSCredentials(_accessKeyId, _accessKeySecret);
        return new RestS3Service(awsCredentials);
    }

    public EmrCluster createEmrCluster(String name, String s3Bucket, int instanceCount) {
        return new EmrCluster(new EmrSettings(name, _accessKeyId, s3Bucket, _privateKeyName, instanceCount), _accessKeySecret);
    }

    public InstanceGroup createInstanceGroup() {
        return createInstanceGroup(createJEc2());
    }

    public InstanceGroup createInstanceGroup(Jec2 ec2) {
        return new InstanceGroupImpl(ec2);
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
