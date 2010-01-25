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
package datameer.awstasks.ec2.ant;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;

import com.xerox.amazonws.ec2.InstanceType;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;

import datameer.awstasks.ec2.InstanceGroup;
import datameer.awstasks.ec2.InstanceGroupImpl;

public class Ec2StartTask extends AbstractEc2Task {

    private String _ami;
    private int _instanceCount;
    private String _privateKeyName;

    private String _instanceType;
    private String _userData;
    private String _availabilityZone;

    public void setAmi(String ami) {
        _ami = ami;
    }

    public String getAmi() {
        return _ami;
    }

    public void setInstanceCount(int instanceCount) {
        _instanceCount = instanceCount;
    }

    public int getInstanceCount() {
        return _instanceCount;
    }

    public void setPrivateKeyName(String privateKeyName) {
        _privateKeyName = privateKeyName;
    }

    public String getPrivateKeyName() {
        return _privateKeyName;
    }

    public void setInstanceType(String instanceType) {
        _instanceType = instanceType;
    }

    public String getInstanceType() {
        return _instanceType;
    }

    public void setUserData(String userData) {
        _userData = userData;
    }

    public String getUserData() {
        return _userData;
    }

    public void setAvailabilityZone(String availabilityZone) {
        _availabilityZone = availabilityZone;
    }

    public String getAvailabilityZone() {
        return _availabilityZone;
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            LaunchConfiguration launchConfiguration = new LaunchConfiguration(_ami, _instanceCount, _instanceCount);
            launchConfiguration.setKeyName(_privateKeyName);
            launchConfiguration.setSecurityGroup(Arrays.asList("default", _groupName));
            if (_userData != null) {
                launchConfiguration.setUserData(_userData.getBytes());
            }
            if (_instanceType != null) {
                InstanceType instanceType = InstanceType.valueOf(_instanceType.toUpperCase());
                launchConfiguration.setInstanceType(instanceType);
            }
            launchConfiguration.setAvailabilityZone(_availabilityZone);
            instanceGroup.startup(launchConfiguration, TimeUnit.MINUTES, 10);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
