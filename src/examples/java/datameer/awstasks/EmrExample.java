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

import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.util.Ec2Configuration;

public class EmrExample {

    public static void main(String[] args) throws IOException, InterruptedException {
        String clusterName = "my-cluster";
        String s3Bucket = "testbucket";
        // have your aws access data
        // String accessKeyId = null;
        // String accessKeySecret = null;
        // String privateKeyName;
        // EmrSettings settings = new EmrSettings(clusterName, accessKeyId, privateKeyName,
        // s3Bucket, 5);
        // EmrCluster emrCluster = new EmrCluster(settings, accessKeySecret);

        // or alternatively use the Ec2Configuration
        Ec2Configuration ec2Configuration = new Ec2Configuration(); // searches for ec2.properties
        EmrCluster emrCluster = ec2Configuration.createEmrCluster(clusterName, s3Bucket, 5);

        // start a new flow
        emrCluster.startup();

        // or connect to an existing
        // emrCluster.setName("myCluster"); //by name
        // emrCluster.connect();
        //        
        // emrCluster.connect("j-9RFYACO46O2Z");//by id

        // execute a flow step
        String remoteInputPath = "/emr/input";
        String remoteOutputPath = "/emr/output";
        String inputUri = "s3n://" + s3Bucket + remoteInputPath;
        String outputUri = "s3n://" + s3Bucket + remoteOutputPath;
        emrCluster.executeJobStep("wordcount-" + System.currentTimeMillis(), new File("lib/test/hadoop-0.18.3-examples.jar"), inputUri, outputUri);

        // shut the flow down
        emrCluster.shutdown();
    }
}
