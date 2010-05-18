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
package datameer.awstasks.aws.s3;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;
import datameer.awstasks.util.IoUtil;

public class S3BucketTest extends AbstractAwsIntegrationTest {

    public static final String AWS_TEST_BUCKET = "aws.test.bucket";

    @Test
    public void testCreateDeleteBucket() throws Exception {
        AmazonS3 s3Service = _ec2Conf.createS3Service();
        String bucketName = AWS_TEST_BUCKET;
        removeBucket(s3Service, bucketName);
        assertFalse(s3Service.doesBucketExist(bucketName));

        s3Service.createBucket(bucketName);
        assertTrue(s3Service.doesBucketExist(bucketName));

        s3Service.deleteBucket(bucketName);
        assertFalse(s3Service.doesBucketExist(bucketName));
    }

    private void removeBucket(AmazonS3 s3Service, String bucketName) {
        boolean bucketExists = s3Service.doesBucketExist(bucketName);
        if (bucketExists) {
            List<S3ObjectSummary> objectSummaries = s3Service.listObjects(bucketName).getObjectSummaries();
            for (S3ObjectSummary objectSummary : objectSummaries) {
                s3Service.deleteObject(bucketName, objectSummary.getKey());
            }
            s3Service.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUploadFile_ExistsFile() throws Exception {
        AmazonS3 s3Service = _ec2Conf.createS3Service();
        s3Service.createBucket(AWS_TEST_BUCKET);
        String remotePath = "/tmp/build.xml";
        IoUtil.uploadFile(s3Service, AWS_TEST_BUCKET, new File("build.xml"), remotePath);
        assertTrue(IoUtil.existsFile(s3Service, AWS_TEST_BUCKET, remotePath));
        assertFalse(IoUtil.existsFile(s3Service, AWS_TEST_BUCKET, remotePath + "/dwefwfe"));
        assertTrue(IoUtil.existsFile(s3Service, AWS_TEST_BUCKET, ".p4tickets"));
    }
}
