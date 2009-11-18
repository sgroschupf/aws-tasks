package com.dm.awstasks;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class InstanceGroupImplIntegTest extends AbstractIntegrationTest {

    @Test
    public void testStartWithoutWait() throws Exception {
        String imageId = "ami-5059be39";
        Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);

        int instanceCount = 1;
        LaunchConfiguration launchConfiguration = new LaunchConfiguration(imageId, instanceCount, instanceCount);
        // launchConfiguration.setInstanceType(InstanceType.DEFAULT);// default is small
        // launchConfiguration.setUserData(null);// see
        // http://docs.amazonwebservices.com/AWSEC2/2008-02-01/DeveloperGuide/
        // launchConfiguration.setAvailabilityZone("");
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2, launchConfiguration);

        // startup
        ReservationDescription reservationDescription = instanceGroup.startup();
        assertEquals(instanceCount, reservationDescription.getInstances().size());
        checkInstanceMode(reservationDescription, "pending");

        // shutdown
        instanceGroup.shutdown();
        checkInstanceMode(instanceGroup.getCurrentReservationDescription(), "shutting-down");
    }

    @Test
    public void testStartWithWaitOnRunning() throws Exception {
        String imageId = "ami-5059be39";
        Jec2 ec2 = new Jec2(_accessKeyId, _accessKeySecret);

        int instanceCount = 1;
        LaunchConfiguration launchConfiguration = new LaunchConfiguration(imageId, instanceCount, instanceCount);
        InstanceGroup instanceGroup = new InstanceGroupImpl(ec2, launchConfiguration);

        // startup
        ReservationDescription reservationDescription = instanceGroup.startup(TimeUnit.MINUTES, 1);
        assertEquals(instanceCount, reservationDescription.getInstances().size());
        checkInstanceMode(reservationDescription, "running");

        // shutdown
        instanceGroup.shutdown();
        checkInstanceMode(instanceGroup.getCurrentReservationDescription(), "shutting-down");
    }

    private void checkInstanceMode(ReservationDescription reservationDescription, String mode) {
        List<Instance> instances = reservationDescription.getInstances();
        for (Instance instance : instances) {
            assertEquals(mode, instance.getState());
        }
    }
}
