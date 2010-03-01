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

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.junit.Test;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;
import datameer.awstasks.util.IoUtil;

public class S3BucketTest extends AbstractAwsIntegrationTest {

    public static final String AWS_TEST_BUCKET = "aws.test.bucket";

    @Test
    public void testCreateDeleteBucket() throws Exception {
        S3Service s3Service = _ec2Conf.createS3Service();
        String bucketName = AWS_TEST_BUCKET;
        cleanBucket(s3Service, bucketName);
        s3Service.deleteBucket(bucketName);
        assertNull(s3Service.getBucket(bucketName));

        s3Service.createBucket(bucketName);
        assertNotNull(s3Service.getBucket(bucketName));

        s3Service.deleteBucket(bucketName);
        assertNull(s3Service.getBucket(bucketName));
    }

    private void cleanBucket(S3Service s3Service, String bucketName) throws S3ServiceException {
        S3Bucket bucket = s3Service.getBucket(bucketName);
        S3Object[] s3Objects = s3Service.listObjects(bucket);
        for (S3Object s3Object : s3Objects) {
            s3Service.deleteObject(bucket, s3Object.getKey());
        }
    }

    @Test
    public void testUploadFile_ExistsFile() throws Exception {
        S3Service s3Service = _ec2Conf.createS3Service();
        s3Service.getOrCreateBucket(AWS_TEST_BUCKET);
        String remotePath = "/tmp/build.xml";
        IoUtil.uploadFile(s3Service, AWS_TEST_BUCKET, new File("build.xml"), remotePath);
        assertTrue(IoUtil.existsFile(s3Service, AWS_TEST_BUCKET, remotePath));
    }
}
