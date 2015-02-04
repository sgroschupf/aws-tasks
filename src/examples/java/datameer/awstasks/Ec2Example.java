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
package datameer.awstasks;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import awstasks.com.amazonaws.services.ec2.AmazonEC2;
import awstasks.com.amazonaws.services.ec2.model.RunInstancesRequest;

import datameer.awstasks.aws.ec2.InstanceGroup;
import datameer.awstasks.aws.ec2.ssh.SshClient;
import datameer.awstasks.util.Ec2Configuration;
import datameer.awstasks.util.IoUtil;

public class Ec2Example {

    public static void main(String[] args) throws IOException {
        // have your aws access data
        // File privateKeyFile = null;
        // String accessKeyId = null;
        // String accessKeySecret = null;
        // String privateKeyName = null;
        //
        // AmazonEC2 ec2 = new AmazonEC2Client(new BasicAWSCredentials(accessKeyId,
        // accessKeySecret));
        // InstanceGroup instanceGroup = new InstanceGroupImpl(ec2);

        // or alternatively use the Ec2Configuration
        Ec2Configuration ec2Configuration = new Ec2Configuration(); // searches for ec2.properties
        String privateKeyName = ec2Configuration.getPrivateKeyName();
        File privateKeyFile = new File(ec2Configuration.getPrivateKeyFile());

        AmazonEC2 ec2 = ec2Configuration.createEc2();
        InstanceGroup instanceGroup = ec2Configuration.createInstanceGroup(ec2);

        // startup an instance group
        RunInstancesRequest launchConfiguration = new RunInstancesRequest("ami-5059be39", 5, 5);
        launchConfiguration.setKeyName(privateKeyName);
        instanceGroup.launch(launchConfiguration, TimeUnit.MINUTES, 5);

        // or connect to a running one
        instanceGroup.connectTo("securityGroup");

        // scp/ssh - to all instances
        SshClient sshClient = instanceGroup.createSshClient("ubuntu", privateKeyFile);
        sshClient.uploadFile(new File("/etc/someFile"), "~/uploadedFile");
        sshClient.uploadFile(new File("/etc/someDir"), "~/");
        sshClient.downloadFile("~/someFile", new File("/etc/someFileDownloaded"), false);
        sshClient.executeCommand("ls -l ~/", IoUtil.closeProtectedStream(System.out));

        // or to specific instances
        sshClient.uploadFile(new File("/etc/someFile"), "~/uploadedFile", new int[] { 0 });
        sshClient.uploadFile(new File("/etc/someFile2"), "~/uploadedFile", new int[] { 1, 2, 3, 4 });
        sshClient.executeCommand("start-master.sh -v", IoUtil.closeProtectedStream(System.out), new int[] { 0 });
        sshClient.executeCommand("start-nodes.sh -v", IoUtil.closeProtectedStream(System.out), new int[] { 1, 2, 3, 4 });

        // shutdown ec2 instances
        instanceGroup.terminate();

    }
}
