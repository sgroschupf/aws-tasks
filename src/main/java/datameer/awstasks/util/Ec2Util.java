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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;

public class Ec2Util {

    public static List<Instance> findByGroup(AmazonEC2 ec2, String securityGroup, boolean includeMultipleReservations, InstanceStateName... instanceStates) {
        List<Reservation> reservations = ec2.describeInstances(new DescribeInstancesRequest().withFilters(Filters.groupName(securityGroup), Filters.instanceStates(instanceStates))).getReservations();
        if (reservations.size() > 1 && !includeMultipleReservations) {
            throw new IllegalArgumentException("found more then one (" + reservations.size() + ") running instance groups (/reservations) for the given security group '" + securityGroup
                    + "' with instances in '" + Arrays.asList(instanceStates) + "' mode");
        } else if (reservations.isEmpty()) {
            return null;
        }
        List<Instance> instances = new ArrayList<Instance>();
        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
        }
        return instances;
    }

    public static Collection<String> getSecurityGroups(List<Instance> instances) {
        Set<String> groups = new HashSet<String>();
        for (Instance instance : instances) {
            List<GroupIdentifier> securityGroups = instance.getSecurityGroups();
            for (GroupIdentifier groupIdentifier : securityGroups) {
                groups.add(groupIdentifier.getGroupName());
            }
        }
        return groups;
    }

    public static List<IpPermission> getPermissions(AmazonEC2 ec2, Collection<String> securityGroupNames, Filter... filters) {
        List<SecurityGroup> securityGroups = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(securityGroupNames).withFilters(filters)).getSecurityGroups();
        List<IpPermission> ipPermissions = new ArrayList<IpPermission>(3);
        for (SecurityGroup groupDescription : securityGroups) {
            ipPermissions.addAll(groupDescription.getIpPermissions());
        }
        return ipPermissions;
    }

    public static String[] toStrings(Enum<?>... enumInstances) {
        String[] names = new String[enumInstances.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = enumInstances[i].toString();
        }
        return names;
    }

    public static List<String> toIds(List<Instance> instances) {
        List<String> ids = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            ids.add(instance.getInstanceId());
        }
        return ids;
    }

    public static List<String> toPublicDns(List<Instance> instances) {
        List<String> dns = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            dns.add(instance.getPublicDnsName());
        }
        return dns;
    }

    public static List<Instance> reloadInstanceDescriptions(AmazonEC2 ec2, List<Instance> instances) {
        List<Reservation> reservations = ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(toIds(instances))).getReservations();
        List<Instance> updatedInstanceDescriptioins = new ArrayList<Instance>();
        for (Reservation reservation : reservations) {
            updatedInstanceDescriptioins.addAll(reservation.getInstances());
        }
        return updatedInstanceDescriptioins;
    }

    public static Reservation reloadReservation(AmazonEC2 ec2, Reservation reservation) {
        List<Reservation> reservations = ec2.describeInstances(new DescribeInstancesRequest().withFilters(Filters.reservationId(reservation.getReservationId()))).getReservations();
        if (reservations.size() != 1) {
            throw new IllegalStateException("do not found resevervation with id '" + reservation.getReservationId() + "': " + reservations);
        }
        return reservations.get(0);
    }

    public static List<String> getInstanceIds(Reservation reservationDescription) {
        List<Instance> instances = reservationDescription.getInstances();
        List<String> instanceIds = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }
        return instanceIds;
    }

    public static boolean groupExists(AmazonEC2 ec2, String groupName) {
        try {
            DescribeSecurityGroupsResult groups = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(groupName));
            return !groups.getSecurityGroups().isEmpty();
        } catch (AmazonServiceException e) {
            if (e.getErrorCode().equals("InvalidGroup.NotFound")) {
                return false;
            }
            throw e;
        }
    }

}
