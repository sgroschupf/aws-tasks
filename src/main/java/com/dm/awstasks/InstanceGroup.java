package com.dm.awstasks;

import java.util.concurrent.TimeUnit;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ReservationDescription;

/**
 * An amazon ec2 group of instances for a given AMI (Amazon machine image).
 */
public interface InstanceGroup {

    ReservationDescription startup() throws EC2Exception;

    void shutdown() throws EC2Exception;

    /**
     * 
     * @param timeUnit
     * @param waitTime
     * @return a reservation description with the current instances
     * @throws EC2Exception
     */
    ReservationDescription waitUntilServerUp(TimeUnit timeUnit, int waitTime) throws EC2Exception;
}
