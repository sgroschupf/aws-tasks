package com.dm.awstasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.dm.awstasks.ssh.ScpUploader;
import com.dm.awstasks.ssh.ScpUploaderImpl;
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

    public InstanceGroupImpl(Jec2 ec2, ReservationDescription reservationDescription) {
        _ec2 = ec2;
        _launchConfiguration = null;
        _reservationDescription = reservationDescription;
    }

    @Override
    public ReservationDescription startup() throws EC2Exception {
        return startup(null, 0);
    }

    @Override
    public ReservationDescription startup(TimeUnit timeUnit, long time) throws EC2Exception {
        // TODO jz: return boolean and don't throw ex, the reservationDescription can be retrieved
        // via getCurrentR...()
        if (_reservationDescription != null) {
            throw new IllegalStateException("instance group already started");
        }
        LOG.info(String.format("starting %d to %d instances...", _launchConfiguration.getMinCount(), _launchConfiguration.getMaxCount()));
        _reservationDescription = _ec2.runInstances(_launchConfiguration);
        LOG.info(String.format("triggered start of %d instances: %s", _reservationDescription.getInstances().size(), getInstanceIds(_reservationDescription)));
        if (timeUnit != null) {
            waitUntilServerUp(timeUnit, time);
            LOG.info(String.format("started %d instances: %s / %s", _reservationDescription.getInstances().size(), getInstanceIds(_reservationDescription), getInstanceDns(_reservationDescription)));
        }
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

    public static List<String> getInstanceDns(ReservationDescription reservationDescription) {
        List<Instance> instances = reservationDescription.getInstances();
        List<String> instanceIds = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            instanceIds.add(instance.getDnsName());
        }
        return instanceIds;
    }

    private ReservationDescription waitUntilServerUp(TimeUnit timeUnit, long waitTime) throws EC2Exception {
        long end = System.currentTimeMillis() + timeUnit.toMillis(waitTime);
        boolean notAllUp;
        do {
            notAllUp = false;
            try {
                long sleepTime = 10000;
                LOG.info(String.format("waiting on instances to run. Sleeping %d ms. zzz...", sleepTime));
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            updateReservationDescription();
            List<Instance> startingInstances = _reservationDescription.getInstances();
            for (Instance instance : startingInstances) {
                if (!"running".equals(instance.getState())) {
                    notAllUp = true;
                }
            }
        } while (notAllUp && System.currentTimeMillis() < end);
        if (notAllUp) {
            throw new EC2Exception("not all instance are in state 'running'");
        }
        return _reservationDescription;
    }

    @Override
    public ReservationDescription getCurrentReservationDescription() throws EC2Exception {
        updateReservationDescription();
        return _reservationDescription;
    }

    private synchronized void updateReservationDescription() throws EC2Exception {
        _reservationDescription = _ec2.describeInstances(getInstanceIds(_reservationDescription)).get(0);
    }

    public ScpUploader createScpUploader(File privateKey, String username) throws EC2Exception {
        return createScpUploader(privateKey, username, null);
    }

    @Override
    public ScpUploader createScpUploader(File privateKey, String username, int[] instanceIndex) throws EC2Exception {
        updateReservationDescription();
        checkInstanceMode(_reservationDescription.getInstances(), "running");
        List<String> instanceDns = getInstanceDns(_reservationDescription);
        if (instanceIndex != null) {
            for (int i = 0; i < instanceIndex.length; i++) {
                instanceDns.remove(instanceIndex[i]);
            }
        }
        return new ScpUploaderImpl(privateKey, instanceDns, username);
    }

    private void checkInstanceMode(List<Instance> instances, String desiredMode) {
        for (Instance instance : instances) {
            if (!instance.getState().equals(desiredMode)) {
                throw new IllegalStateException("instance " + instance.getInstanceId() + " is not in mode '" + desiredMode + "' but in mode '" + instance.getState() + "'");
            }
        }
    }

}
