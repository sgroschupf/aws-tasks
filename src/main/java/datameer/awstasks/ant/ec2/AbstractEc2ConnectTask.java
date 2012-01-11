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

import org.apache.tools.ant.BuildException;

import com.amazonaws.services.ec2.AmazonEC2;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.InstanceGroupImpl;

public abstract class AbstractEc2ConnectTask extends AbstractEc2Task {

    @Override
    public final void doExecute() throws BuildException {
        AmazonEC2 ec2 = createEc2();
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);
        try {
            instanceGroup.connectTo(_groupName);
            doExecute(ec2, instanceGroup);
        } catch (Exception e) {
            LOG.info("execution " + getClass().getSimpleName() + " with groupName '" + _groupName + "' failed: " + e.getMessage());
            throw new BuildException(e);
        }
    }

    protected abstract void doExecute(AmazonEC2 ec2, InstanceGroup instanceGroup) throws Exception;
}
