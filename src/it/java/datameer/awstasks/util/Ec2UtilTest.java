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

import static org.fest.assertions.Assertions.*;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import awstasks.com.amazonaws.services.ec2.model.Instance;
import awstasks.com.amazonaws.services.ec2.model.InstanceStateName;

import datameer.awstasks.aws.ec2.AbstractEc2IntegrationInteractionTest;
import datameer.awstasks.aws.ec2.InstanceGroup;

public class Ec2UtilTest extends AbstractEc2IntegrationInteractionTest {

    @Test
    public void testFindByGroup() {
        List<Instance> instances = Ec2Util.findByGroup(_ec2, TEST_SECURITY_GROUP, false, InstanceStateName.Running);
        assertThat(instances).hasSize(INSTANCE_COUNT);

        // start a second instance-group within the same security group
        InstanceGroup scndInstanceGroup = _ec2Conf.createInstanceGroup(_ec2);
        scndInstanceGroup.launch(createLaunchConfiguration(1));
        try {
            instances = Ec2Util.findByGroup(_ec2, TEST_SECURITY_GROUP, false, InstanceStateName.Running, InstanceStateName.Pending);
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }

        instances = Ec2Util.findByGroup(_ec2, TEST_SECURITY_GROUP, true, InstanceStateName.Running, InstanceStateName.Pending);
        assertThat(instances).hasSize(INSTANCE_COUNT + 1);
        scndInstanceGroup.terminate();
    }

}
