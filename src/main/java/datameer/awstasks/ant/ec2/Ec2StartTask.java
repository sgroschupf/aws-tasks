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
package datameer.awstasks.ant.ec2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.tools.ant.BuildException;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;

import datameer.awstasks.aws.ec2.GroupPermission;
import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.util.Ec2Util;

public class Ec2StartTask extends AbstractEc2Task {

    private String _ami;
    private int _instanceCount;
    private String _privateKeyName;

    private String _instanceType;
    private String _instanceName;
    private String _userData;
    private String _availabilityZone;
    private String _kernelId;
    private String _ramDiskId;
    private String _groupDescription;
    private int _maxStartTime = 10;// in minutes
    private boolean _reuseRunningInstances = false;

    private List<GroupPermission> _groupPermissions = new ArrayList<GroupPermission>();

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

    public void setInstanceName(String instanceName) {
        _instanceName = instanceName;
    }

    public String getInstanceName() {
        return _instanceName;
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

    public void setKernelId(String kernelId) {
        _kernelId = kernelId;
    }

    public String getKernelId() {
        return _kernelId;
    }

    public void setRamDiskId(String ramDiskId) {
        _ramDiskId = ramDiskId;
    }

    public String getRamDiskId() {
        return _ramDiskId;
    }

    public void setGroupDescription(String groupDescription) {
        _groupDescription = groupDescription;
    }

    public String getGroupDescription() {
        return _groupDescription;
    }

    public void addGroupPermission(GroupPermission groupPermission) {
        _groupPermissions.add(groupPermission);
    }

    public int getMaxStartTime() {
        return _maxStartTime;
    }

    public void setMaxStartTime(int maxStartTime) {
        _maxStartTime = maxStartTime;
    }

    public void setReuseRunningInstances(boolean reuseRunningInstances) {
        _reuseRunningInstances = reuseRunningInstances;
    }

    public boolean isReuseRunningInstances() {
        return _reuseRunningInstances;
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        AmazonEC2 ec2 = createEc2();
        try {
            boolean instancesRunning = Ec2Util.findByGroup(ec2, _groupName, false, InstanceStateName.Pending, InstanceStateName.Running) != null;
            if (!isReuseRunningInstances() && instancesRunning) {
                throw new IllegalStateException("found already running instances for group '" + _groupName + "'");
            }
            if (!Ec2Util.groupExists(ec2, _groupName)) {
                System.out.println("group '" + _groupName + "' does not exists - creating it");
                String groupDescription = getGroupDescription();
                if (groupDescription == null) {
                    throw new BuildException("must specify groupDescription");
                }
                ec2.createSecurityGroup(new CreateSecurityGroupRequest(_groupName, groupDescription));
            }

            List<String> securityGroups = Arrays.asList("default", _groupName);
            List<IpPermission> existingPermissions = Ec2Util.getPermissions(ec2, securityGroups);
            for (GroupPermission groupPermission : _groupPermissions) {
                if (groupPermission.getToPort() == -1) {
                    groupPermission.setToPort(groupPermission.getFromPort());
                }
                if (!permissionExists(groupPermission, existingPermissions)) {
                    System.out.println("did not found permission '" + groupPermission + "' - creating it...");
                    ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest().withGroupName(_groupName).withIpPermissions(groupPermission.toIpPermission()));
                }
            }

            InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
            RunInstancesRequest launchConfiguration = new RunInstancesRequest(_ami, _instanceCount, _instanceCount);
            if (_kernelId != null) {
                launchConfiguration.setKernelId(_kernelId);
            }
            if (_ramDiskId != null) {
                launchConfiguration.setKernelId(_ramDiskId);
            }
            launchConfiguration.setKeyName(_privateKeyName);
            launchConfiguration.setSecurityGroups(securityGroups);
            if (_userData != null) {
                launchConfiguration.setUserData(Base64.encodeBase64String(_userData.getBytes()));
            }
            if (_instanceType != null) {
                launchConfiguration.setInstanceType(_instanceType);
            }
            launchConfiguration.setPlacement(new Placement(_availabilityZone));
            if (instancesRunning) {
                instanceGroup.connectTo(_groupName);
            } else {
                instanceGroup.startup(launchConfiguration, TimeUnit.MINUTES, _maxStartTime);
                if (_instanceName != null) {
                    System.out.println("tagging instances with name '" + _instanceName + " [<idx>]'");
                    int idx = 1;
                    for (Instance instance : instanceGroup.getInstances(false)) {
                        CreateTagsRequest createTagsRequest = new CreateTagsRequest();
                        createTagsRequest.withResources(instance.getInstanceId()) //
                                .withTags(new Tag("Name", _instanceName + " [" + idx + "]"));
                        ec2.createTags(createTagsRequest);
                        idx++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("execution " + getClass().getSimpleName() + " with groupName '" + _groupName + "' failed: " + e.getMessage());
            throw new BuildException(e);
        }
    }

    private boolean permissionExists(GroupPermission groupPermission, List<IpPermission> existingPermissions) {
        for (IpPermission ipPermission : existingPermissions) {
            if (groupPermission.matches(ipPermission)) {
                return true;
            }
        }
        return false;
    }
}
