package com.dm.awstasks.ec2.ant;

import org.apache.tools.ant.BuildException;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.InstanceGroupImpl;
import com.xerox.amazonws.ec2.Jec2;

public class Ec2StopTask extends AbstractEc2Task {

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        Jec2 ec2 = new Jec2(_accessKey, _accessSecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            instanceGroup.connectTo(_groupName);
            instanceGroup.shutdown();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
