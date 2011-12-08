package datameer.awstasks.ant.s3.model;

import org.apache.tools.ant.Project;

import com.amazonaws.services.s3.AmazonS3;

public class DeleteObjectCommand extends S3Command {

    private String _bucket;
    private String _key;

    public String getBucket() {
        return _bucket;
    }

    public void setBucket(String bucket) {
        _bucket = bucket;
    }

    public String getKey() {
        return _key;
    }

    public void setKey(String key) {
        _key = key;
    }

    @Override
    public void execute(Project project, AmazonS3 s3Service) {
        s3Service.deleteObject(_bucket, _key);
        System.out.println("deleting '" + _key + "' in bucket '" + _bucket + "'");
    }
}
