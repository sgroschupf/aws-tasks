package com.dm.awstasks.ec2;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.dm.awstasks.util.Ec2Util;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class InstanceGroupImplIntegTest extends AbstractEc2IntegrationTest {

    @Test
    public void testStartWithoutWait() throws Exception {
        Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

        // startup
        ReservationDescription reservationDescription = instanceGroup.startup(createLaunchConfiguration(1));
        assertTrue(instanceGroup.isAssociated());
        assertEquals(1, reservationDescription.getInstances().size());
        checkInstanceMode(reservationDescription, "pending");

        // shutdown
        instanceGroup.shutdown();
        checkInstanceMode(Ec2Util.reloadReservationDescription(ec2, reservationDescription), "shutting-down");
        assertFalse(instanceGroup.isAssociated());
    }

    @Test
    public void testStartWithWaitOnRunning() throws Exception {
        Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

        // startup
        ReservationDescription reservationDescription = instanceGroup.startup(createLaunchConfiguration(1), TimeUnit.MINUTES, 1);
        assertTrue(instanceGroup.isAssociated());
        assertEquals(1, reservationDescription.getInstances().size());
        checkInstanceMode(reservationDescription, "running");

        // shutdown
        instanceGroup.shutdown();
        checkInstanceMode(Ec2Util.reloadReservationDescription(ec2, reservationDescription), "shutting-down");
        assertFalse(instanceGroup.isAssociated());
    }

    @Test
    public void testConnectToReservation() throws Exception {
        Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);
        InstanceGroup instanceGroup1 = new InstanceGroupImpl(ec2);
        InstanceGroup instanceGroup2 = new InstanceGroupImpl(ec2);

        // startup
        ReservationDescription reservationDescription = instanceGroup1.startup(createLaunchConfiguration(1), TimeUnit.MINUTES, 1);
        assertTrue(instanceGroup1.isAssociated());

        // connect
        instanceGroup2.connectTo(reservationDescription);
        assertTrue(instanceGroup2.isAssociated());

        // shutdown
        instanceGroup2.shutdown();
        checkInstanceMode(Ec2Util.reloadReservationDescription(ec2, reservationDescription), "shutting-down");
        assertFalse(instanceGroup2.isAssociated());
    }

    @Test
    public void testConnectToGroup() throws Exception {
        Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);
        InstanceGroup instanceGroup1 = new InstanceGroupImpl(ec2);
        InstanceGroup instanceGroup2 = new InstanceGroupImpl(ec2);

        // startup
        ReservationDescription reservationDescription = instanceGroup1.startup(createLaunchConfiguration(1), TimeUnit.MINUTES, 1);
        assertTrue(instanceGroup1.isAssociated());

        // connect
        instanceGroup2.connectTo("default");
        assertTrue(instanceGroup2.isAssociated());

        // shutdown
        instanceGroup2.shutdown();
        checkInstanceMode(Ec2Util.reloadReservationDescription(ec2, reservationDescription), "shutting-down");
        assertFalse(instanceGroup2.isAssociated());
    }

    private void checkInstanceMode(ReservationDescription reservationDescription, String mode) {
        List<Instance> instances = reservationDescription.getInstances();
        for (Instance instance : instances) {
            assertEquals(mode, instance.getState());
        }
    }
}
