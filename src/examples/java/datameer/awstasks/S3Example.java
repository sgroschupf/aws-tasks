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

import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

import datameer.awstasks.util.Ec2Configuration;

public class S3Example {

    public static void main(String[] args) throws Exception {
        // have your aws access data
        // String accessKeyId = null;
        // String accessKeySecret = null;
        //        
        // AWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, accessKeySecret);
        // AmazonS3 s3Service = new AmazonS3Client(awsCredentials);

        // or alternatively use the Ec2Configuration
        Ec2Configuration ec2Configuration = new Ec2Configuration(); // searches for ec2.properties
        AmazonS3 s3Service = ec2Configuration.createS3Service();
        Bucket s3Bucket = s3Service.createBucket("aExampleBucket");
        List<Bucket> buckets = s3Service.listBuckets();
        for (Bucket bucket : buckets) {
            System.out.println(bucket.getName());
        }
        s3Service.deleteBucket(s3Bucket.getName());
    }
}
