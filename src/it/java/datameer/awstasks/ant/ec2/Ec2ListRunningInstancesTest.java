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

import org.apache.tools.ant.Project;
import org.junit.Test;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;

public class Ec2ListRunningInstancesTest extends AbstractAwsIntegrationTest {

    @Test
    public void testSshExecutionToAllInstances() throws Exception {
        Project project = new Project();
        Ec2ListRunningInstances task = new Ec2ListRunningInstances();
        task.setProject(project);
        task.setAccessKey(_ec2Conf.getAccessKey());
        task.setAccessSecret(_ec2Conf.getAccessSecret());

        task.execute();
    }
}
