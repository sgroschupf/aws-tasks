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

import com.amazonaws.services.ec2.AmazonEC2;

import datameer.awstasks.aws.ec2.InstanceGroup;

public class Ec2ShutdownTask extends AbstractEc2ConnectTask {

    private boolean _stopOnly = false;

    public boolean isStopOnly() {
        return _stopOnly;
    }

    public void setStopOnly(boolean stopOnly) {
        _stopOnly = stopOnly;
    }

    @Override
    protected void doExecute(AmazonEC2 ec2, InstanceGroup instanceGroup) throws Exception {
        LOG.info("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        if (_stopOnly) {
            instanceGroup.stop();
        } else {
            instanceGroup.terminate();
        }
    }
}
