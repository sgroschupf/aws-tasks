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
package datameer.awstasks.aws.ec2;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import awstasks.com.amazonaws.services.ec2.AmazonEC2;
import awstasks.com.amazonaws.services.ec2.model.Instance;
import awstasks.com.amazonaws.services.ec2.model.InstanceStateName;

import datameer.awstasks.util.Ec2Util;

public class AbstractEc2IntegrationInteractionTest extends AbstractEc2IntegrationTest {

    protected static InstanceGroupImpl _instanceGroup;
    protected static AmazonEC2 _ec2;
    protected static boolean CLUSTER_ALREADY_RUNNING = false;// manual switch to avoid startup time
    protected static int INSTANCE_COUNT = 2;

    @BeforeClass
    public static void startupInstanceGroup() {
        _ec2 = _ec2Conf.createEc2();
        _instanceGroup = new InstanceGroupImpl(_ec2);

        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("try to use existing instance group");
            List<Instance> instances = Ec2Util.findByGroup(_ec2, TEST_SECURITY_GROUP, false, InstanceStateName.Pending, InstanceStateName.Running);
            if (instances == null) {
                LOG.warn("reservation description with running instances NOT found - starting instance group");
            } else {
                LOG.warn("reservation description with running instances FOUND - using instance group");
                _instanceGroup.connectTo(TEST_SECURITY_GROUP);
            }
        }
        if (!_instanceGroup.isAssociated()) {
            _instanceGroup.launch(createLaunchConfiguration(INSTANCE_COUNT), TimeUnit.MINUTES, 15);
        }
    }

    @AfterClass
    public static void shutdownInstanceGroup() {
        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("don't shutdown instance group");
        } else {
            _instanceGroup.terminate();
        }
    }

}
