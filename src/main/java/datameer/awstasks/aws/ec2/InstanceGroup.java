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

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;

import datameer.awstasks.aws.ec2.ssh.SshClient;

/**
 * An amazon ec2 group of instances for a given AMI (Amazon Machine Image).
 */
public interface InstanceGroup {

    /**
     * Starts the configured {@linkplain InstanceGroup}. After a call of this method, the concerning
     * instances are likely to be in 'pending' mode.
     * 
     * @param launchConfiguration
     * @return a {@link ReservationDescription} with (snapshot) information about the running
     *         instances
     */
    Reservation startup(RunInstancesRequest launchConfiguration);

    /**
     * Starts the configured {@linkplain InstanceGroup} and waits until all instances are in
     * 'running' mode. If after the specified waiting time the instances are still not running a
     * {@link EC2Exception} is thrown.
     * 
     * @param launchConfiguration
     * @param timeUnit
     *            the unit of the time parameter
     * @param time
     *            maximum time to wait (average startup time depends on image but is around 1 min)
     * @return a {@link ReservationDescription} with (snapshot) information about the running
     *         instances
     */
    Reservation startup(RunInstancesRequest launchConfiguration, TimeUnit timeUnit, long time);

    /**
     * Connect to already running instances of a reservation.
     * 
     * @param reservationDescription
     * @throws EC2Exception
     */
    void connectTo(Reservation reservationDescription);

    /**
     * Connect to running instances in a security group. If more then one reservation for that group
     * with instances in mode 'running' exists, a {@link EC2Exception} is thrown.
     * 
     * @param groupName
     * @throws EC2Exception
     */
    void connectTo(String groupName);

    /**
     * Shut all ec2 instances in this group down.
     * 
     * @throws EC2Exception
     */
    void shutdown();

    /**
     * 
     * @return true if the if the instance group has been started or connected and not been shutdown
     *         yet
     */
    boolean isAssociated();

    List<Instance> getInstances(boolean updateBefore);

    SshClient createSshClient(String username, File privateKey);

    SshClient createSshClient(String username, String password);

    int instanceCount();

}
