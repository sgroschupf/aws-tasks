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

import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;
import datameer.com.google.common.base.Preconditions;

public abstract class AbstractEc2IntegrationTest extends AbstractAwsIntegrationTest {

    public static final String TEST_SECURITY_GROUP = "aws-tasks.test";
    public static final String TEST_EBS = "aws-test-ebs";
    public static final String TEST_AMI = "ami-205fba49";
    public static final String TEST_AMI_WITH_EBS = "ami-84db39ed";
    public static final String TEST_USERNAME = "root";

    protected static RunInstancesRequest createLaunchConfiguration(int instanceCount) {
        return createLaunchConfiguration(TEST_AMI, instanceCount);
    }

    protected static RunInstancesRequest createEbsLaunchConfiguration(int instanceCount) {
        return createLaunchConfiguration(TEST_AMI_WITH_EBS, instanceCount);
    }

    protected static RunInstancesRequest createLaunchConfiguration(String imageId, int instanceCount) {
        RunInstancesRequest runRequest = new RunInstancesRequest(imageId, instanceCount, instanceCount);
        runRequest.setKeyName(_ec2Conf.getPrivateKeyName());
        runRequest.setSecurityGroups(Arrays.asList(TEST_SECURITY_GROUP, "default"));
        return runRequest;
    }

    protected Volume findEbsVolume(AmazonEC2 ec2) {
        DescribeVolumesResult describeVolumes = ec2.describeVolumes(new DescribeVolumesRequest().withFilters(new Filter().withName("tag:Name").withValues(TEST_EBS)));
        List<Volume> volumes = describeVolumes.getVolumes();
        Preconditions.checkArgument(volumes.size() == 1, "expected 1 EBS volume with name tag '%s' but got %s", TEST_EBS, volumes);
        return volumes.get(0);
    }
}
