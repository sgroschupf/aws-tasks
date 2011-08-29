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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

import datameer.awstasks.ant.AbstractAwsTask;

public class AbstractEc2Task extends AbstractAwsTask {

    protected String _groupName;

    public void setGroupName(String name) {
        _groupName = name;
    }

    public String getName() {
        return _groupName;
    }

    public AmazonEC2 createEc2() {
        return new AmazonEC2Client(new BasicAWSCredentials(_accessKey, _accessSecret));
    }

}
