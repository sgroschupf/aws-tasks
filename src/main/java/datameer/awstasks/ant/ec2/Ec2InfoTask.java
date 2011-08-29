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
import java.util.List;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;

import datameer.awstasks.aws.ec2.InstanceGroup;

public class Ec2InfoTask extends AbstractEc2ConnectTask {

    private List<Task> _commands = new ArrayList<Task>();

    public void addEcho(Echo task) {
        _commands.add(task);
    }

    @Override
    protected void execute(AmazonEC2 ec2, InstanceGroup instanceGroup) throws Exception {
        System.out.println("executing " + getClass().getSimpleName() + " for group '" + _groupName + "'");
        List<Instance> instances = instanceGroup.getInstances(false);
        getProject().setProperty("instances.count", instances.size() + "");
        StringBuilder instancesHosts = new StringBuilder();
        for (int i = 0; i < instances.size(); i++) {
            getProject().setProperty("instance.host." + i, instances.get(i).getPublicDnsName());
            getProject().setProperty("instance.aim." + i, instances.get(i).getImageId());
            getProject().setProperty("instance.id." + i, instances.get(i).getInstanceId());
            getProject().setProperty("instance.type." + i, instances.get(i).getInstanceType());
            getProject().setProperty("instance.state." + i, instances.get(i).getState().getName());
            instancesHosts.append(instances.get(i).getPublicDnsName());
            if (i < instances.size() - 1) {
                instancesHosts.append(',');
            }
        }
        getProject().setProperty("instances.hosts", instancesHosts.toString());

        for (Task task : _commands) {
            task.setProject(getProject());
            task.execute();
        }
    }

}
