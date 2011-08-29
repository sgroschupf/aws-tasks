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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;

import datameer.awstasks.util.Ec2Util;

public class InstanceGroupImplIntegTest extends AbstractEc2IntegrationTest {

    @Test
    public void testStartWithoutWait() throws Exception {
        AmazonEC2 ec2 = _ec2Conf.createEc2();
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

        // startup
        int instanceCount = 1;
        Reservation reservation = instanceGroup.startup(createLaunchConfiguration(instanceCount));
        assertEquals(instanceCount, instanceGroup.instanceCount());
        assertTrue(instanceGroup.isAssociated());
        assertEquals(instanceCount, reservation.getInstances().size());
        checkInstanceMode(reservation.getInstances(), InstanceStateName.Pending);

        // shutdown
        instanceGroup.shutdown();
        Thread.sleep(500);
        checkInstanceMode(Ec2Util.reloadInstanceDescriptions(ec2, reservation.getInstances()), InstanceStateName.ShuttingDown, InstanceStateName.Terminated);
        assertFalse(instanceGroup.isAssociated());
    }

    @Test
    public void testStartWithWaitOnRunning() throws Exception {
        AmazonEC2 ec2 = _ec2Conf.createEc2();
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

        // startup
        Reservation reservation = instanceGroup.startup(createLaunchConfiguration(1), TimeUnit.MINUTES, 10);
        assertTrue(instanceGroup.isAssociated());
        assertEquals(1, reservation.getInstances().size());
        checkInstanceMode(reservation.getInstances(), InstanceStateName.Running);

        // shutdown
        instanceGroup.shutdown();
        Thread.sleep(1000);
        checkInstanceMode(Ec2Util.reloadInstanceDescriptions(ec2, reservation.getInstances()), InstanceStateName.ShuttingDown, InstanceStateName.Terminated);
        assertFalse(instanceGroup.isAssociated());
    }

    @Test
    public void testConnectToReservation() throws Exception {
        AmazonEC2 ec2 = _ec2Conf.createEc2();
        InstanceGroup instanceGroup1 = new InstanceGroupImpl(ec2);
        InstanceGroup instanceGroup2 = new InstanceGroupImpl(ec2);

        // startup
        Reservation reservation = instanceGroup1.startup(createLaunchConfiguration(1), TimeUnit.MINUTES, 10);
        assertTrue(instanceGroup1.isAssociated());

        // connect
        instanceGroup2.connectTo(reservation);
        assertTrue(instanceGroup2.isAssociated());

        // shutdown
        instanceGroup2.shutdown();
        Thread.sleep(500);
        checkInstanceMode(Ec2Util.reloadInstanceDescriptions(ec2, reservation.getInstances()), InstanceStateName.ShuttingDown, InstanceStateName.Terminated);
        assertFalse(instanceGroup2.isAssociated());
    }

    @Test
    public void testConnectToGroup() throws Exception {
        AmazonEC2 ec2 = _ec2Conf.createEc2();
        InstanceGroup instanceGroup1 = new InstanceGroupImpl(ec2);
        InstanceGroup instanceGroup2 = new InstanceGroupImpl(ec2);

        // startup
        Reservation reservation = instanceGroup1.startup(createLaunchConfiguration(1), TimeUnit.MINUTES, 10);
        assertTrue(instanceGroup1.isAssociated());

        // connect
        instanceGroup2.connectTo(TEST_SECURITY_GROUP);
        assertTrue(instanceGroup2.isAssociated());

        // shutdown
        instanceGroup2.shutdown();
        Thread.sleep(500);
        checkInstanceMode(Ec2Util.reloadInstanceDescriptions(ec2, reservation.getInstances()), InstanceStateName.ShuttingDown, InstanceStateName.Terminated);
        assertFalse(instanceGroup2.isAssociated());
    }

    private void checkInstanceMode(List<Instance> instances, InstanceStateName... instanceModes) {
        for (Instance instance : instances) {
            boolean inOneOfDesiredStates = false;
            for (InstanceStateName instanceStateName : instanceModes) {
                if (instanceStateName.toString().equals(instance.getState().getName())) {
                    inOneOfDesiredStates = true;
                }
            }
            if (!inOneOfDesiredStates) {
                fail("instance is in mode '" + instance.getState() + "' but should be (one of) '" + Arrays.asList(instanceModes) + "'");
            }
        }

    }
}
