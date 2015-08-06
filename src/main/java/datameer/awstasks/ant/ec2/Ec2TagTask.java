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

import awstasks.com.amazonaws.services.ec2.AmazonEC2;
import awstasks.com.amazonaws.services.ec2.model.Tag;
import awstasks.com.amazonaws.services.ec2.model.Instance;
import awstasks.com.amazonaws.services.ec2.model.CreateTagsRequest;
import datameer.awstasks.aws.ec2.InstanceGroup;
import java.util.Map;
import java.util.HashMap;

public class Ec2TagTask extends AbstractEc2ConnectTask {

    private String _key;
    private String _value;

    public void setTag(String tag)
    {
        String[] parts = tag.split("=");
        _key = parts[0];
        _value = parts[1];
    }

    @Override
    protected void doExecute(AmazonEC2 ec2, InstanceGroup instanceGroup) throws Exception {
        LOG.info("executing " + getClass().getSimpleName() + " with groupName '" + _groupName + "'");
        for (Instance instance : instanceGroup.getInstances(false)) {
            CreateTagsRequest createTagsRequest = new CreateTagsRequest();
            createTagsRequest.withResources(instance.getInstanceId())
                    .withTags(new Tag(_key, _value));
            ec2.createTags(createTagsRequest);
        }
    }
}
