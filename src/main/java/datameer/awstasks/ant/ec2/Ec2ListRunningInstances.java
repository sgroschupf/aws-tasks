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
package datameer.awstasks.ant.ec2;

import java.util.List;
import java.util.Set;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;

import datameer.com.google.common.base.Objects;
import datameer.com.google.common.collect.ImmutableList;
import datameer.com.google.common.collect.LinkedHashMultimap;
import datameer.com.google.common.collect.Lists;
import datameer.com.google.common.collect.Multimap;

public class Ec2ListRunningInstances extends AbstractEc2Task {

    private List<FilterEntry> _filterEntries = Lists.newArrayList();
    private String _nameFilter;

    public String getNameFilter() {
        return _nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        _nameFilter = nameFilter;
    }

    public void addFilter(FilterEntry filterEntry) {
        _filterEntries.add(filterEntry);
    }

    @Override
    protected void doExecute(AmazonEC2 ec2) {
        DescribeInstancesRequest describeRequest = createDescribeRequest();
        List<Filter> filters = describeRequest.getFilters();
        System.out.println("Setup following filters:");
        for (Filter filter : filters) {
            System.out.println("\t" + filter);
        }
        List<Reservation> reservations = ec2.describeInstances(describeRequest).getReservations();
        System.out.println("Retrieved following reservations:");
        int runningInstanceGroups = 0;
        int runningInstances = 0;
        for (Reservation reservation : reservations) {
            List<Instance> instances = reservation.getInstances();
            runningInstanceGroups++;
            runningInstances += instances.size();
            System.out.println(reservation.getGroupNames() + ":");
            for (Instance instance : instances) {
                System.out.println("\t" + getInstanceName(instance) + " (" + instance.getInstanceId() + " / " + instance.getState().getName() + "): " + instance.getPublicDnsName());
            }
        }
        System.out.println("Found " + runningInstances + " running instances in " + runningInstanceGroups + " instance groups");
    }

    private DescribeInstancesRequest createDescribeRequest() {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        Multimap<String, String> keyToValuesMap = combineFiltersWithSameName();
        Set<String> keySet = keyToValuesMap.keySet();
        for (String key : keySet) {
            describeInstancesRequest.withFilters(new Filter(key, ImmutableList.copyOf(keyToValuesMap.get(key))));
        }
        return describeInstancesRequest;
    }

    private Multimap<String, String> combineFiltersWithSameName() {
        Multimap<String, String> keyToValuesMap = LinkedHashMultimap.create();
        for (FilterEntry filterEntry : _filterEntries) {
            keyToValuesMap.put(filterEntry.getName(), filterEntry.getValue());
        }
        return keyToValuesMap;
    }

    private Object getInstanceName(Instance instance) {
        for (Tag tag : instance.getTags()) {
            if (tag.getKey().equalsIgnoreCase("name")) {
                return tag.getValue();
            }
        }
        return null;
    }

    public static class FilterEntry {

        private String _name;
        private String _value;

        public void setName(String name) {
            this._name = name;
        }

        public String getName() {
            return _name;
        }

        public void setValue(String value) {
            this._value = value;
        }

        public String getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).addValue(_name).addValue(_value).toString();
        }
    }
}
