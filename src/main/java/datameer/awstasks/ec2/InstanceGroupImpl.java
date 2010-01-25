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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.GroupDescription;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.TerminatingInstanceDescription;
import com.xerox.amazonws.ec2.GroupDescription.IpPermission;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

import datameer.awstasks.ec2.ssh.SshClient;
import datameer.awstasks.ec2.ssh.SshClientImpl;
import datameer.awstasks.ssh.JschRunner;
import datameer.awstasks.util.Ec2Util;

public class InstanceGroupImpl implements InstanceGroup {

    private static Logger LOG = Logger.getLogger(InstanceGroupImpl.class);

    private final Jec2 _ec2;
    private ReservationDescription _reservationDescription;

    public InstanceGroupImpl(Jec2 ec2) {
        _ec2 = ec2;
    }

    @Override
    public void connectTo(String groupName) throws EC2Exception {
        checkEc2Association(false);
        LOG.info(String.format("connecting to instances of group '%s'", groupName));
        _reservationDescription = Ec2Util.findByGroup(_ec2, groupName, "running");
        if (_reservationDescription == null) {
            throw new EC2Exception("no instances of group '" + groupName + "' running");
        }
    }

    @Override
    public void connectTo(ReservationDescription reservationDescription) throws EC2Exception {
        checkEc2Association(false);
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
        checkEc2Association(false);
        LOG.info(String.format("starting %d to %d instances in groups %s...", launchConfiguration.getMinCount(), launchConfiguration.getMaxCount(), launchConfiguration.getSecurityGroup()));
        _reservationDescription = _ec2.runInstances(launchConfiguration);
        List<String> instanceIds = Ec2Util.getInstanceIds(_reservationDescription);
        LOG.info(String.format("triggered start of %d instances: %s", _reservationDescription.getInstances().size(), instanceIds));
        if (timeUnit != null) {
            waitUntilServerUp(timeUnit, time);
            LOG.info(String.format("started %d instances: %s / %s", _reservationDescription.getInstances().size(), instanceIds, getPublicDns(_reservationDescription)));
        }
        return _reservationDescription;
    }

    private void checkEc2Association(boolean shouldBeAssociated) {
        if (shouldBeAssociated && !isAssociated()) {
            throw new IllegalStateException("instance group is not yet associated with ec2 instances");
        }
        if (!shouldBeAssociated && isAssociated()) {
            throw new IllegalStateException("instance group already associated with ec2 instances");
        }
    }

    @Override
    public boolean isAssociated() {
        return _reservationDescription != null;
    }

    @Override
    public int instanceCount() {
        checkEc2Association(true);
        return _reservationDescription.getInstances().size();
    }

    @Override
    public void shutdown() throws EC2Exception {
        checkEc2Association(true);
        List<TerminatingInstanceDescription> terminatedInstances = _ec2.terminateInstances(Ec2Util.getInstanceIds(_reservationDescription));
        _reservationDescription = null;
        LOG.info("stopped " + terminatedInstances.size() + " instances");
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
    public ReservationDescription getReservationDescription(boolean updateBefore) throws EC2Exception {
        checkEc2Association(true);
        if (updateBefore) {
            updateReservationDescription();
        }
        return _reservationDescription;
    }

    private static List<String> getPublicDns(ReservationDescription reservationDescription) {
        List<Instance> instances = reservationDescription.getInstances();
        List<String> instanceIds = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            instanceIds.add(instance.getDnsName());
        }
        return instanceIds;
    }

    private synchronized void updateReservationDescription() throws EC2Exception {
        _reservationDescription = Ec2Util.reloadReservationDescription(_ec2, _reservationDescription);
    }

    @Override
    public SshClient createSshClient(String username, File privateKey) throws EC2Exception {
        checkEc2Association(true);
        updateReservationDescription();
        checkInstanceMode(_reservationDescription.getInstances(), "running");
        List<String> instanceDns = getPublicDns(_reservationDescription);
        checkSshPermissions();
        checkSshConnection(username, instanceDns, privateKey);
        return new SshClientImpl(username, privateKey, instanceDns);
    }

    private void checkSshConnection(String username, List<String> instanceDns, File privateKey) throws EC2Exception {
        LOG.info("checking ssh connections");
        for (String dns : instanceDns) {
            JschRunner runner = new JschRunner(username, dns);
            runner.setKeyfile(privateKey.getAbsolutePath());
            runner.setTrust(true);
            try {
                runner.testConnect(TimeUnit.MINUTES.toMillis(5));
            } catch (IOException e) {
                throw new EC2Exception(e.getMessage());
            }
        }
    }

    private void checkSshPermissions() throws EC2Exception {
        GroupPermission sshPermission = GroupPermission.createStandardSsh();
        List<IpPermission> tcpPermissions = getPermissions(sshPermission.getProtocol());
        if (tcpPermissions.isEmpty()) {
            throw new EC2Exception("no permission for '" + sshPermission + "' set");
        }
        boolean foundMatching = false;
        for (IpPermission ipPermission : tcpPermissions) {
            if (ipPermission.getFromPort() == sshPermission.getFromPort() && ipPermission.getToPort() == sshPermission.getToPort()) {
                foundMatching = true;
            }
        }
        if (!foundMatching) {
            throw new EC2Exception("found permission for protocol '" + sshPermission.getProtocol() + " but with diverse ports (need " + sshPermission + ")");
        }
    }

    private List<IpPermission> getPermissions(String protocol) throws EC2Exception {
        List<GroupDescription> securityGroups = _ec2.describeSecurityGroups(_reservationDescription.getGroups());
        List<IpPermission> ipPermissions = new ArrayList<IpPermission>(3);
        for (GroupDescription groupDescription : securityGroups) {
            List<IpPermission> permissions = groupDescription.getPermissions();
            for (IpPermission ipPermission : permissions) {
                if (ipPermission.getProtocol().equals(protocol)) {
                    ipPermissions.add(ipPermission);
                }
            }
        }
        return ipPermissions;
    }

    private void checkInstanceMode(List<Instance> instances, String desiredMode) {
        for (Instance instance : instances) {
            if (!instance.getState().equals(desiredMode)) {
                throw new IllegalStateException("instance " + instance.getInstanceId() + " is not in mode '" + desiredMode + "' but in mode '" + instance.getState() + "'");
            }
        }
    }

}
