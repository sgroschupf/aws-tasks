package com.dm.awstasks.ec2.ant;

import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;

public class Ec2StartTask extends AbstractEc2Task {

    private String _ami;
    private int _instanceCount;
    private String _privateKeyName;

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

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            LaunchConfiguration launchConfiguration = new LaunchConfiguration(_ami, _instanceCount, _instanceCount);
            launchConfiguration.setKeyName(_privateKeyName);
            instanceGroup.startup(launchConfiguration, TimeUnit.MINUTES, 10);
        } catch (EC2Exception e) {
            throw new BuildException(e);
        }
    }
}
