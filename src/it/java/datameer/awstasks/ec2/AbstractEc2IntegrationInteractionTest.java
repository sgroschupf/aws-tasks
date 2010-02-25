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
package datameer.awstasks.ec2;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;

import datameer.awstasks.aws.ec2.InstanceGroupImpl;
import datameer.awstasks.util.Ec2Util;

public class AbstractEc2IntegrationInteractionTest extends AbstractEc2IntegrationTest {

    protected static InstanceGroupImpl _instanceGroup;
    protected static Jec2 _ec2;
    protected static boolean CLUSTER_ALREADY_RUNNING = false;// to avoid startup time
    protected static int INSTANCE_COUNT = 2;

    @BeforeClass
    public static void startupInstanceGroup() throws EC2Exception {
        _ec2 = _ec2Conf.createJEc2();
        _instanceGroup = new InstanceGroupImpl(_ec2);

        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("try to use existing instance group");
            ReservationDescription reservationDescription = Ec2Util.findByGroup(_ec2, TEST_SECURITY_GROUP, "running");
            if (reservationDescription == null) {
                LOG.warn("reservation description with running instances NOT found - starting instance group");
            } else {
                LOG.warn("reservation description with running instances FOUND - using instance group");
                _instanceGroup.connectTo(reservationDescription);
            }
        }
        if (!_instanceGroup.isAssociated()) {
            _instanceGroup.startup(createLaunchConfiguration(INSTANCE_COUNT), TimeUnit.MINUTES, 5);
        }
    }

    @AfterClass
    public static void shutdownInstanceGroup() throws EC2Exception {
        if (CLUSTER_ALREADY_RUNNING) {
            LOG.info("don't shutdown instance group");
        } else {
            _instanceGroup.shutdown();
        }
    }

}
