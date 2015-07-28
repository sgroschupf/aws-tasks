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

import org.apache.log4j.NDC;
import org.apache.tools.ant.BuildException;

import awstasks.com.amazonaws.auth.BasicAWSCredentials;
import awstasks.com.amazonaws.regions.Region;
import awstasks.com.amazonaws.regions.Regions;
import awstasks.com.amazonaws.services.ec2.AmazonEC2;
import awstasks.com.amazonaws.services.ec2.AmazonEC2Client;
import datameer.awstasks.ant.AbstractAwsTask;

public abstract class AbstractEc2Task extends AbstractAwsTask {

    protected String _groupName;
    protected String _region;

    public void setGroupName(String name) {
        _groupName = name;
    }

    public String getName() {
        return _groupName;
    }

    public String getRegion() {
        return _region;
    }

    public void setRegion(String region) {
        _region = region;
    }

    private AmazonEC2 createEc2() {
        AmazonEC2Client ec2Client = new AmazonEC2Client(new BasicAWSCredentials(_accessKey, _accessSecret));
        if (_region != null && !_region.trim().isEmpty()) {
            ec2Client.setRegion(Region.getRegion(Regions.valueOf(_region.toUpperCase())));
        }
        LOG.info("connect to region " + _region);
        return ec2Client;
    }

    @Override
    public final void execute() throws BuildException {
        if (NDC.getDepth() <= 0) {
            NDC.push(_groupName);
        }
        validate();
        doExecute(createEc2());
    }

    protected abstract void doExecute(AmazonEC2 ec2);

    protected void validate() {
        // subclasses may override
    }

}
