package datameer.awstasks;

import java.io.File;
import java.io.IOException;

import org.jets3t.service.S3ServiceException;

import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceException;

import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.util.Ec2Configuration;

public class EmrExample {

    public static void main(String[] args) throws IOException, S3ServiceException, InterruptedException, AmazonElasticMapReduceException {
        String s3Bucket = "testbucket";
        // have your aws access data
        // String accessKeyId = null;
        // String accessKeySecret = null;
        // EmrCluster emrCluster =new EmrCluster(accessKeyId, accessKeySecret, s3Bucket);

        // or alternatively use the Ec2Configuration
        Ec2Configuration ec2Configuration = new Ec2Configuration(); // searches for ec2.properties
        String privateKeyName = ec2Configuration.getPrivateKeyName();
        EmrCluster emrCluster = ec2Configuration.createEmrCluster(s3Bucket);

        // start a new flow
        emrCluster.startup(2, privateKeyName);

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
