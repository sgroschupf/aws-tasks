package com.dm.awstasks.ec2;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.dm.awstasks.util.Ec2Util;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;

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
