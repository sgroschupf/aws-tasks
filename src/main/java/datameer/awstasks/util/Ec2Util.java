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
package datameer.awstasks.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.ec2.model.InstanceStateName;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.GroupDescription;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.GroupDescription.IpPermission;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class Ec2Util {

    public static ReservationDescription findByGroup(Jec2 ec2, String securityGroup, InstanceStateName... instanceStates) throws EC2Exception {
        List<ReservationDescription> reservationDescriptions = ec2.describeInstances(Collections.EMPTY_LIST);
        List<ReservationDescription> matchingReservationDescriptions = new ArrayList<ReservationDescription>(3);
        for (ReservationDescription reservationDescription : reservationDescriptions) {
            if (reservationDescription.getGroups().contains(securityGroup) && isInOneOfStates(reservationDescription.getInstances().get(0).getState(), instanceStates)) {
                matchingReservationDescriptions.add(reservationDescription);
            }
        }
        if (matchingReservationDescriptions.size() > 1) {
            throw new EC2Exception("found more then one instance group for security group '" + securityGroup + "' and with instances in '" + Arrays.asList(instanceStates) + "' mode");
        } else if (matchingReservationDescriptions.isEmpty()) {
            return null;
        }
        return matchingReservationDescriptions.get(0);
    }

    private static boolean isInOneOfStates(String state, InstanceStateName... instanceStates) {
        for (InstanceStateName instanceStateName : instanceStates) {
            if (state.equalsIgnoreCase(instanceStateName.name())) {
                return true;
            }
        }
        return false;
    }

    public static ReservationDescription reloadReservationDescription(Jec2 ec2, ReservationDescription reservationDescription) throws EC2Exception {
        List<String> instanceIds = getInstanceIds(reservationDescription);
        List<ReservationDescription> reservationDescriptions = ec2.describeInstances(instanceIds);
        if (reservationDescriptions.size() > 1) {
            throw new IllegalStateException("found more then one reservation description for thes instances: " + instanceIds);
        }
        if (reservationDescriptions.isEmpty()) {
            throw new IllegalStateException("found no reservation description for these instances: " + instanceIds);
        }
        return reservationDescriptions.get(0);
    }

    public static List<String> getInstanceIds(ReservationDescription reservationDescription) {
        List<Instance> instances = reservationDescription.getInstances();
        List<String> instanceIds = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }
        return instanceIds;
    }

    public static void main(String[] args) throws IOException, EC2Exception {
        Jec2 ec2 = new Ec2Configuration().createJEc2();
        ec2.describeSecurityGroups(Arrays.asList("asda"));
    }

    public static boolean groupExists(Jec2 ec2, String groupName) {
        try {
            ec2.describeSecurityGroups(Arrays.asList(groupName));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<IpPermission> getPermissions(Jec2 ec2, List<String> groupNames) throws EC2Exception {
        List<GroupDescription> securityGroups = ec2.describeSecurityGroups(groupNames);
        List<IpPermission> ipPermissions = new ArrayList<IpPermission>(3);
        for (GroupDescription groupDescription : securityGroups) {
            List<IpPermission> permissions = groupDescription.getPermissions();
            for (IpPermission ipPermission : permissions) {
                ipPermissions.add(ipPermission);
            }
        }
        return ipPermissions;
    }

}
