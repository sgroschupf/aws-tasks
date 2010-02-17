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

import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Bucket;

import datameer.awstasks.util.Ec2Configuration;

public class S3Example {

    public static void main(String[] args) throws Exception {
        // have your aws access data
        // String accessKeyId = null;
        // String accessKeySecret = null;
        //        
        // AWSCredentials awsCredentials = new AWSCredentials(accessKeyId, accessKeySecret);
        // S3Service s3Service = new RestS3Service(awsCredentials);

        // or alternatively use the Ec2Configuration
        Ec2Configuration ec2Configuration = new Ec2Configuration(); // searches for ec2.properties
        S3Service s3Service = ec2Configuration.createS3Service();
        S3Bucket s3Bucket = s3Service.createBucket("aExampleBucket");
        S3Bucket[] buckets = s3Service.listAllBuckets();
        for (S3Bucket bucket : buckets) {
            System.out.println(bucket.getName());
        }
        s3Service.deleteBucket(s3Bucket);
    }
}
