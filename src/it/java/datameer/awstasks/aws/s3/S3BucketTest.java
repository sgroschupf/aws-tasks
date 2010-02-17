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

import org.jets3t.service.S3Service;
import org.junit.Test;

import datameer.awstasks.aws.AbstractAwsIntegrationTest;

public class S3BucketTest extends AbstractAwsIntegrationTest {

    @Test
    public void testCreateDeleteBucket() throws Exception {
        S3Service s3Service = _ec2Conf.createS3Service();
        String bucketName = "aws.test.bucket";
        assertNull(s3Service.getBucket(bucketName));

        s3Service.createBucket(bucketName);
        assertNotNull(s3Service.getBucket(bucketName));

        s3Service.deleteBucket(bucketName);
        assertNull(s3Service.getBucket(bucketName));
    }
}
