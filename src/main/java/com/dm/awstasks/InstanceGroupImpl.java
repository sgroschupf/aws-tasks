package com.dm.awstasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.dm.awstasks.ssh.ScpUploader;
import com.dm.awstasks.ssh.ScpUploaderImpl;
import com.dm.awstasks.ssh.SshExecutor;
import com.dm.awstasks.ssh.SshExecutorImpl;
import com.dm.awstasks.util.Ec2Util;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.TerminatingInstanceDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class InstanceGroupImpl implements InstanceGroup {

    private static Logger LOG = Logger.getLogger(InstanceGroupImpl.class);

    private final Jec2 _ec2;
    private ReservationDescription _reservationDescription;

    public InstanceGroupImpl(Jec2 ec2) {
        _ec2 = ec2;
    }

    @Override
    public void connectTo(String groupName) throws EC2Exception {
        checkAssociation(false);
        LOG.info(String.format("connecting to group '%s'", groupName));
        _reservationDescription = Ec2Util.findByGroup(_ec2, groupName, "running");
    }

    @Override
    public void connectTo(ReservationDescription reservationDescription) throws EC2Exception {
        checkAssociation(false);
        LOG.info(String.format("connecting to reservation '%s'", reservationDescription.getReservationId()));
        _reservationDescription = reservationDescription;
        updateReservationDescription();
    }

    @Override
    public ReservationDescription startup(LaunchConfiguration launchConfiguration) throws EC2Exception {
        return startup(launchConfiguration, null, 0);
    }

    @Override
    public ReservationDescription startup(LaunchConfiguration launchConfiguration, TimeUnit timeUnit, long time) throws EC2Exception {
        checkAssociation(false);
        LOG.info(String.format("starting %d to %d instances...", launchConfiguration.getMinCount(), launchConfiguration.getMaxCount()));
        _reservationDescription = _ec2.runInstances(launchConfiguration);
        List<String> instanceIds = Ec2Util.getInstanceIds(_reservationDescription);
        LOG.info(String.format("triggered start of %d instances: %s", _reservationDescription.getInstances().size(), instanceIds));
        if (timeUnit != null) {
            waitUntilServerUp(timeUnit, time);
            LOG.info(String.format("started %d instances: %s / %s", _reservationDescription.getInstances().size(), instanceIds, getInstanceDns(_reservationDescription)));
        }
        return _reservationDescription;
    }

    private void checkAssociation(boolean shouldBeAssociated) throws EC2Exception {
        if (shouldBeAssociated && !isAssociated()) {
            throw new IllegalStateException("instance group is not yet associated with ec2 instances");
        }
        if (!shouldBeAssociated && isAssociated()) {
            throw new IllegalStateException("instance group already associated with ec2 instances");
        }
    }

    @Override
    public void shutdown() throws EC2Exception {
        checkAssociation(true);
        List<TerminatingInstanceDescription> terminatedInstances = _ec2.terminateInstances(Ec2Util.getInstanceIds(_reservationDescription));
        _reservationDescription = null;
        LOG.info("stopped " + terminatedInstances.size() + " instances");
    }

    private static List<String> getInstanceDns(ReservationDescription reservationDescription) {
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
                LOG.info(String.format("wait on instances to enter 'running' mode. Sleeping %d ms. zzz...", sleepTime));
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
        checkAssociation(true);
        updateReservationDescription();
        return _reservationDescription;
    }

    private synchronized void updateReservationDescription() throws EC2Exception {
        _reservationDescription = Ec2Util.reloadReservationDescription(_ec2, _reservationDescription);
    }

    public ScpUploader createScpUploader(File privateKey, String username) throws EC2Exception {
        return createScpUploader(privateKey, username, null);
    }

    @Override
    public ScpUploader createScpUploader(File privateKey, String username, int[] instanceIndex) throws EC2Exception {
        checkAssociation(true);
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

    @Override
    public SshExecutor createSshExecutor(File privateKey, String username) throws EC2Exception {
        return createSshExecutor(privateKey, username, null);
    }

    @Override
    public SshExecutor createSshExecutor(File privateKey, String username, int[] instanceIndex) throws EC2Exception {
        checkAssociation(true);
        updateReservationDescription();
        checkInstanceMode(_reservationDescription.getInstances(), "running");
        List<String> instanceDns = getInstanceDns(_reservationDescription);
        if (instanceIndex != null) {
            for (int i = 0; i < instanceIndex.length; i++) {
                instanceDns.remove(instanceIndex[i]);
            }
        }
        return new SshExecutorImpl(privateKey, instanceDns, username);
    }

    private void checkInstanceMode(List<Instance> instances, String desiredMode) {
        for (Instance instance : instances) {
            if (!instance.getState().equals(desiredMode)) {
                throw new IllegalStateException("instance " + instance.getInstanceId() + " is not in mode '" + desiredMode + "' but in mode '" + instance.getState() + "'");
            }
        }
    }

    @Override
    public boolean isAssociated() throws EC2Exception {
        return _reservationDescription != null;
    }

}
