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
package datameer.awstasks.util;

import java.io.File;

import awstasks.com.amazonaws.services.s3.AmazonS3;
import awstasks.com.amazonaws.services.s3.model.AmazonS3Exception;

public class S3Util {

    public static void uploadFile(AmazonS3 s3Service, String bucket, File file, String remotePath) {
        if (remotePath.startsWith("/")) {
            remotePath = remotePath.substring(1);
        }
        s3Service.putObject(bucket, remotePath, file);
    }

    public static boolean existsFile(AmazonS3 s3Service, String bucketName, String remotePath) {
        if (remotePath.startsWith("/")) {
            remotePath = remotePath.substring(1);
        }
        try {
            s3Service.getObjectAcl(bucketName, remotePath);
            return true;
        } catch (AmazonS3Exception e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
    }

    public static String normalizeBucketName(String name) {
        name = name.toLowerCase();
        name = name.replace(' ', '-');
        name = name.replace('.', '-');
        return name;
    }

}
