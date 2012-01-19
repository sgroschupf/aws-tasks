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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.google.common.base.Preconditions;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.util.Ec2Util;

public class Ec2StartTask extends AbstractEc2Task {

    private String _instanceIds;
    private int _maxStartTime = 10;// in minutes
    private boolean _reuseRunningInstances = false;

    public void setInstanceIds(String instanceIds) {
        _instanceIds = instanceIds;
    }

    public String getInstanceIds() {
        return _instanceIds;
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
    public void doExecute() throws BuildException {
        LOG.info("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        AmazonEC2 ec2 = createEc2();
        Preconditions.checkArgument(_instanceIds != null && _instanceIds.length() > 0, "no instance ids set");
        List<String> instanceIds = Arrays.asList(_instanceIds.split(","));
        try {
            List<Instance> instances = Ec2Util.getReservation(ec2, instanceIds).getInstances();
            boolean instancesRunning = checkForRunningState(instances);
            if (!isReuseRunningInstances() && instancesRunning) {
                throw new IllegalStateException("found already running instances for group '" + _groupName + "'");
            }
            if (!Ec2Util.groupExists(ec2, _groupName)) {
                throw new BuildException("group '" + _groupName + "' does not exists");
            }

            InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
            if (instancesRunning) {
                instanceGroup.connectTo(_groupName);
            } else {
                instanceGroup.start(instanceIds, TimeUnit.MINUTES, _maxStartTime);
            }
        } catch (Exception e) {
            LOG.error("execution " + getClass().getSimpleName() + " with groupName '" + _groupName + "' failed: " + e.getMessage());
            throw new BuildException(e);
        }
    }

    private boolean checkForRunningState(List<Instance> instances) {
        int aliveCount = 0;
        int terminatedCount = 0;
        for (Instance instance : instances) {
            if (Ec2Util.isAlive(instance)) {
                aliveCount++;
            } else {
                terminatedCount++;
            }
        }
        Preconditions.checkState(aliveCount == 0 || terminatedCount == 0, "Instances %s in mixed state: " + Ec2Util.toStates(instances));
        return false;
    }

}
