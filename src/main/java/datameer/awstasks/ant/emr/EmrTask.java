package datameer.awstasks.ant.emr;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.jets3t.service.S3ServiceException;

import datameer.awstasks.ant.AbstractAwsTask;
import datameer.awstasks.ant.emr.model.EmrCommand;
import datameer.awstasks.ant.emr.model.EmrStartCommand;
import datameer.awstasks.ant.emr.model.EmrStopCommand;
import datameer.awstasks.aws.emr.EmrCluster;

public class EmrTask extends AbstractAwsTask {

    private String _clusterName;
    private String _s3Bucket;
    private List<EmrCommand> _emrCommands = new ArrayList<EmrCommand>();

    public EmrTask() {
        // default constructor - needed by ant
    }

    public String getClusterName() {
        return _clusterName;
    }

    public void setClusterName(String name) {
        _clusterName = name;
    }

    public void setS3Bucket(String bucket) {
        _s3Bucket = bucket;
    }

    public String getS3Bucket() {
        return _s3Bucket;
    }

    public EmrCluster createEmrCluster() throws S3ServiceException {
        EmrCluster emrCluster = new EmrCluster(_accessKey, _accessSecret, _s3Bucket);
        emrCluster.setName(_clusterName);
        return emrCluster;
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName());
        try {
            EmrCluster emrService = createEmrCluster();
            for (EmrCommand emrCommand : _emrCommands) {
                System.out.println("executing " + emrCommand);
                emrCommand.execute(emrService);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public void addStartCluster(EmrStartCommand command) {
        _emrCommands.add(command);
    }

    public void addStopCluster(EmrStopCommand command) {
        _emrCommands.add(command);
    }

}
