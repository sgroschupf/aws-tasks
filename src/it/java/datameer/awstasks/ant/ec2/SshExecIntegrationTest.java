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

import static org.fest.assertions.Assertions.*;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.Project;
import org.junit.Test;

import awstasks.com.amazonaws.services.ec2.model.Instance;

import datameer.awstasks.ant.ec2.model.SshExec;
import datameer.awstasks.aws.ec2.AbstractEc2IntegrationInteractionTest;
import datameer.com.google.common.base.Function;
import datameer.com.google.common.base.Splitter;
import datameer.com.google.common.collect.ImmutableList;
import datameer.com.google.common.collect.Lists;

public class SshExecIntegrationTest extends AbstractEc2IntegrationInteractionTest {

    @Test
    public void testSshExecutionToAllInstances() throws Exception {
        File privateKeyFile = new File(_ec2Conf.getPrivateKeyFile());
        Project project = new Project();

        SshExec sshExec = new SshExec();
        sshExec.setCommand("echo `curl -s http://169.254.169.254/latest/meta-data/local-hostname`");
        sshExec.setOutputProperty("output");

        Ec2SshTask ec2SshTask = new Ec2SshTask();
        ec2SshTask.setProject(project);
        ec2SshTask.setAccessKey(_ec2Conf.getAccessKey());
        ec2SshTask.setAccessSecret(_ec2Conf.getAccessSecret());
        ec2SshTask.setGroupName(TEST_SECURITY_GROUP);
        ec2SshTask.setUsername(TEST_USERNAME);
        ec2SshTask.setKeyFile(privateKeyFile);
        ec2SshTask.addExec(sshExec);

        ec2SshTask.execute();

        System.out.println(project.getProperty("output"));
        List<String> internalIps = ImmutableList.copyOf(Splitter.on('\n').trimResults().omitEmptyStrings().split(project.getProperty("output")));
        assertThat(internalIps).isEqualTo(Lists.transform(_instanceGroup.getInstances(false), INSTANCE_TO_PRIVATE_DNS_FUNCTION));
    }

    public static Function<Instance, String> INSTANCE_TO_PRIVATE_DNS_FUNCTION = new Function<Instance, String>() {
        @Override
        public String apply(Instance input) {
            return input.getPrivateDnsName();
        }
    };

}
