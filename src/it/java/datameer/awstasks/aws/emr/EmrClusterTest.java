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

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.junit.Before;
import org.junit.Test;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;
import datameer.awstasks.aws.s3.S3BucketTest;
import datameer.awstasks.util.IoUtil;

public class EmrClusterTest extends AbstractAwsIntegrationTest {

    private S3Bucket _s3Bucket;
    private EmrCluster _emrCluster;
    private S3Service _s3Service;
    private static String _jobFlowId;

    @Before
    public void before() throws S3ServiceException {
        _s3Service = _ec2Conf.createS3Service();
        _s3Bucket = _s3Service.getOrCreateBucket(S3BucketTest.AWS_TEST_BUCKET);
        S3Object[] s3Objects = _s3Service.listObjects(_s3Bucket);
        for (S3Object s3Object : s3Objects) {
            _s3Service.deleteObject(_s3Bucket, s3Object.getKey());
        }
        _emrCluster = new EmrCluster(_ec2Conf.getAccessKey(), _ec2Conf.getAccessSecret(), _s3Bucket.getName());
    }

    @Test
    public void testStart() throws Exception {
        assertNull(_emrCluster.getJobFlowId());
        _emrCluster.startup(1, _ec2Conf.getPrivateKeyName());
        assertTrue(_emrCluster.isConnected());
        _jobFlowId = _emrCluster.getJobFlowId();
        assertNotNull(_jobFlowId);
    }

    @Test
    public void testConnect() throws Exception {
        assertNull(_emrCluster.getJobFlowId());
        _emrCluster.connect(_jobFlowId);
        assertTrue(_emrCluster.isConnected());
        assertNotNull(_emrCluster.getJobFlowId());
    }

    @Test
    public void testExecuteJobStep() throws Exception {
        _emrCluster.connect(_jobFlowId);
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
        _emrCluster.executeJobStep("testStep" + System.currentTimeMillis(), jobJar, "wordcount", inputUri, outputUri);

        // check output
        BufferedReader reader = new BufferedReader(new InputStreamReader(_s3Service.getObject(_s3Bucket, remoteOutputPath.substring(1) + "/part-00000").getDataInputStream()));
        assertEquals("F\t1", reader.readLine());
        assertEquals("H\t1", reader.readLine());
        assertEquals("K\t2", reader.readLine());
        assertEquals("L\t1", reader.readLine());
        assertEquals("O\t2", reader.readLine());
        assertEquals("P\t1", reader.readLine());
        reader.close();
    }

    @Test
    public void testShutdwon() throws Exception {
        _emrCluster.connect(_jobFlowId);
        assertTrue(_emrCluster.isConnected());
        _emrCluster.shutdown();
        assertFalse(_emrCluster.isConnected());
        assertNull(_emrCluster.getJobFlowId());

    }
}
