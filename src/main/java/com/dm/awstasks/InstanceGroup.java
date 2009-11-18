package com.dm.awstasks;

import java.util.concurrent.TimeUnit;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ReservationDescription;

/**
 * An amazon ec2 group of instances for a given AMI (Amazon machine image).
 */
public interface InstanceGroup {

    /**
     * Starts the configured {@linkplain InstanceGroup}. After a call of this method, the concerning
     * instances are likely to be in 'pending' mode.
     * 
     * @return a {@link ReservationDescription} with (snapshot) information about the running
     *         instances
     * @throws EC2Exception
     */
    ReservationDescription startup() throws EC2Exception;

    /**
     * Starts the configured {@linkplain InstanceGroup} and waits until all instances are in
     * 'running' mode. If after the specified waiting time the instances are still not running a
     * {@link EC2Exception} is thrown.
     * 
     * @param timeUnit
     *            the unit of the time parameter
     * @param time
     *            maximum time to wait (average startup time depends on image but is around 1 min)
     * @return a {@link ReservationDescription} with (snapshot) information about the running
     *         instances
     * @throws EC2Exception
     *             if wait time is not enough or for any other configuration/communication problem
     */
    ReservationDescription startup(TimeUnit timeUnit, long time) throws EC2Exception;

    ReservationDescription getCurrentReservationDescription() throws EC2Exception;

    void shutdown() throws EC2Exception;

}
