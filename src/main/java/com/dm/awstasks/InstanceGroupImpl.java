package com.dm.awstasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.TerminatingInstanceDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class InstanceGroupImpl implements InstanceGroup {

    private static Logger LOG = Logger.getLogger(InstanceGroupImpl.class);

    private final Jec2 _ec2;
    private final LaunchConfiguration _launchConfiguration;
    private ReservationDescription _reservationDescription;

    public InstanceGroupImpl(Jec2 ec2, LaunchConfiguration launchConfiguration) {
        _ec2 = ec2;
        _launchConfiguration = launchConfiguration;
    }

    @Override
    public ReservationDescription startup() throws EC2Exception {
        if (_reservationDescription != null) {
            throw new IllegalStateException("instance group already started");
        }
        LOG.info(String.format("starting %d to %d instances...", _launchConfiguration.getMinCount(), _launchConfiguration.getMaxCount()));
        _reservationDescription = _ec2.runInstances(_launchConfiguration);
        LOG.info(String.format("started %d instances: %s", _reservationDescription.getInstances().size(), getInstanceIds(_reservationDescription)));
        return _reservationDescription;
    }

    @Override
    public void shutdown() throws EC2Exception {
        List<TerminatingInstanceDescription> terminatedInstances = _ec2.terminateInstances(getInstanceIds(_reservationDescription));
        LOG.info("stopped " + terminatedInstances.size() + " instances");
    }

    public static List<String> getInstanceIds(ReservationDescription reservationDescription) {
        List<Instance> instances = reservationDescription.getInstances();
        List<String> instanceIds = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }
        return instanceIds;
    }

    /**
     * 
     * @param instanceIds
     * @param timeUnit
     * @param waitTime
     * @return true if all instances are in state running
     * @throws EC2Exception
     */
    public ReservationDescription waitUntilServerUp(TimeUnit timeUnit, int waitTime) throws EC2Exception {
        long end = System.currentTimeMillis() + timeUnit.toMillis(waitTime);
        boolean notAllUp;
        do {
            notAllUp = false;
            _reservationDescription = _ec2.describeInstances(getInstanceIds(_reservationDescription)).get(0);
            List<Instance> startingInstances = _reservationDescription.getInstances();
            for (Instance instance : startingInstances) {
                System.out.println(instance.getState());
                if (!"running".equals(instance.getState())) {
                    notAllUp = true;
                }
            }
            try {
                Thread.sleep(Math.max(1000, timeUnit.toMillis(waitTime) / 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (notAllUp && System.currentTimeMillis() < end);
        if (notAllUp) {
            throw new EC2Exception("not all instance are in state 'running'");
        }
        return _reservationDescription;
    }
}
