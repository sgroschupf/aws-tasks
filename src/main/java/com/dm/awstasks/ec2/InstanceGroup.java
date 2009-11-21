package com.dm.awstasks.ec2;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.dm.awstasks.ec2.ssh.Ec2ScpUploader;
import com.dm.awstasks.ec2.ssh.Ec2SshExecutor;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;

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
     * @throws EC2Exception
     */
    ReservationDescription startup(LaunchConfiguration launchConfiguration) throws EC2Exception;

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
     * @throws EC2Exception
     *             if wait time is not enough or for any other configuration/communication problem
     */
    ReservationDescription startup(LaunchConfiguration launchConfiguration, TimeUnit timeUnit, long time) throws EC2Exception;

    /**
     * Connect to already running instances of a reservation.
     * 
     * @param reservationDescription
     * @throws EC2Exception
     */
    void connectTo(ReservationDescription reservationDescription) throws EC2Exception;

    /**
     * Connect to running instances in a security group. If more then one reservation for that group
     * with instances in mode 'running' exists, a {@link EC2Exception} is thrown.
     * 
     * @param groupName
     * @throws EC2Exception
     */
    void connectTo(String groupName) throws EC2Exception;

    /**
     * Shut all ec2 instances in this group down.
     * 
     * @throws EC2Exception
     */
    void shutdown() throws EC2Exception;

    /**
     * 
     * @return true if the if the instance group has been started or connected and not been shutdown
     *         yet
     * @throws EC2Exception
     */
    boolean isAssociated() throws EC2Exception;

    ReservationDescription getCurrentReservationDescription() throws EC2Exception;

    Ec2ScpUploader createScpUploader(File privateKey, String username) throws EC2Exception;

    Ec2SshExecutor createSshExecutor(File privateKey, String username) throws EC2Exception;
}
