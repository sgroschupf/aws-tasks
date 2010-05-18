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
package datameer.awstasks.aws.emr;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;
import datameer.awstasks.aws.emr.EmrCluster.ClusterState;
import datameer.awstasks.aws.emr.EmrCluster.StepFuture;
import datameer.awstasks.aws.emr.EmrCluster.StepMetadata;
import datameer.awstasks.aws.s3.S3BucketTest;
import datameer.awstasks.util.IoUtil;

public class EmrClusterTest extends AbstractAwsIntegrationTest {

    private Bucket _s3Bucket;
    protected EmrCluster _emrCluster;
    private AmazonS3 _s3Service;
    private static String _jobFlowId;

    @Before
    public void before() {
        _s3Service = _ec2Conf.createS3Service();
        _s3Bucket = _s3Service.createBucket(S3BucketTest.AWS_TEST_BUCKET);
        List<S3ObjectSummary> s3Objects = _s3Service.listObjects(_s3Bucket.getName()).getObjectSummaries();
        for (S3ObjectSummary s3ObjectSummary : s3Objects) {
            _s3Service.deleteObject(_s3Bucket.getName(), s3ObjectSummary.getKey());
        }
        EmrSettings settings = new EmrSettings(getClass().getName(), _ec2Conf.getAccessKey(), _ec2Conf.getPrivateKeyName(), _s3Bucket.getName(), 1);
        _emrCluster = new EmrCluster(settings, _ec2Conf.getAccessSecret());
    }

    @Test
    public void testStart() throws Exception {
        assertNull(_emrCluster.getJobFlowId());
        final Set<ClusterState> clusterStates = new HashSet<ClusterState>();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // nothing todo
                }
                clusterStates.add(_emrCluster.getState());
            }
        };
        thread.start();
        _emrCluster.startup();
        assertTrue(clusterStates.toString(), clusterStates.contains(ClusterState.STARTING));
        assertEquals(ClusterState.CONNECTED, _emrCluster.getState());
        _jobFlowId = _emrCluster.getJobFlowId();
        assertNotNull(_jobFlowId);
    }

    @Test
    public void testStart2nClusterWithSameName() throws Exception {
        try {
            _emrCluster.startup();
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
        assertEquals(ClusterState.UNCONNECTED, _emrCluster.getState());

    }

    @Test
    public void testConnectById() throws Exception {
        assertNull(_emrCluster.getJobFlowId());
        _emrCluster.connectById(_jobFlowId);
        assertEquals(ClusterState.CONNECTED, _emrCluster.getState());
        assertNotNull(_emrCluster.getJobFlowId());
    }

    @Test
    public void testConnectByName() throws Exception {
        _emrCluster.connectByName();
        assertEquals(ClusterState.CONNECTED, _emrCluster.getState());
        assertNotNull(_emrCluster.getJobFlowId());
    }

    @Test
    public void testExecuteJobStep() throws Exception {
        _emrCluster.connectByName();
        File jobJar = new File("lib/test/hadoop-0.18.3-examples.jar");

        // prepare input
        File localInputFile = _tempFolder.newFile("inputFile");
        String remoteInputPath = "/emr/input";
        String remoteOutputPath = "/emr/output";
        IoUtil.writeFile(localInputFile, "K O H L", "K O P F");
        IoUtil.uploadFile(_s3Service, _s3Bucket.getName(), localInputFile, remoteInputPath);

        // execute job
        String inputUri = "s3n://" + _s3Bucket.getName() + remoteInputPath;
        String outputUri = "s3n://" + _s3Bucket.getName() + remoteOutputPath;
        StepFuture stepFuture = _emrCluster.executeJobStep("testStep" + System.currentTimeMillis(), jobJar, "wordcount", inputUri, outputUri);
        // assertEquals(2, stepFuture.getStepIndex());// 1 is debug step
        stepFuture.join();

        // check simpledb debuggin information
        StepMetadata stepMetaData = stepFuture.getStepMetaData();
        assertNotNull(stepMetaData);
        assertEquals(_emrCluster.getJobFlowId(), stepMetaData.get(StepMetadata.JOB_FLOW_ID));
        // System.out.println(stepMetaData);

        // check output
        BufferedReader reader = new BufferedReader(new InputStreamReader(_s3Service.getObject(_s3Bucket.getName(), remoteOutputPath.substring(1) + "/part-00000").getObjectContent()));
        assertEquals("F\t1", reader.readLine());
        assertEquals("H\t1", reader.readLine());
        assertEquals("K\t2", reader.readLine());
        assertEquals("L\t1", reader.readLine());
        assertEquals("O\t2", reader.readLine());
        assertEquals("P\t1", reader.readLine());
        reader.close();
    }

    @Test
    public void testExecuteJobStep_ThrottleSafeness() throws Exception {
        int oldRequestInterval = _emrCluster.getRequestInterval();
        _emrCluster.setRequestInterval(1000);// this should produce throttle exceptions
        _emrCluster.connectByName();
        File jobJar = new File("lib/test/hadoop-0.18.3-examples.jar");

        // prepare input
        File localInputFile = _tempFolder.newFile("inputFile");
        String remoteInputPath = "/emr/input";
        String remoteOutputPath = "/emr/output";
        _s3Service.deleteObject(_s3Bucket.getName(), remoteOutputPath);
        IoUtil.writeFile(localInputFile, "K O H L", "K O P F");
        IoUtil.uploadFile(_s3Service, _s3Bucket.getName(), localInputFile, remoteInputPath);

        // execute job
        String inputUri = "s3n://" + _s3Bucket.getName() + remoteInputPath;
        String outputUri = "s3n://" + _s3Bucket.getName() + remoteOutputPath;
        StepFuture stepFuture = _emrCluster.executeJobStep("testStep" + System.currentTimeMillis(), jobJar, "wordcount", inputUri, outputUri);
        stepFuture.join();
        _emrCluster.setRequestInterval(oldRequestInterval);
    }

    @Test
    public void testShutdown() throws Exception {
        _emrCluster.connectByName();
        assertEquals(ClusterState.CONNECTED, _emrCluster.getState());
        _emrCluster.shutdown();
        assertEquals(ClusterState.UNCONNECTED, _emrCluster.getState());
        assertNull(_emrCluster.getJobFlowId());
    }
}
