package datameer.awstasks.util;

import java.io.File;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;

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
            s3Service.getObject(bucketName, remotePath);
            return true;
        } catch (AmazonS3Exception e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
    }

}
