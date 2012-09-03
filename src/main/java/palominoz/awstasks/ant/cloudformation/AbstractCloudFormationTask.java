package palominoz.awstasks.ant.cloudformation;

import org.apache.tools.ant.BuildException;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;

import datameer.awstasks.ant.AbstractAwsTask;

public abstract class AbstractCloudFormationTask extends AbstractAwsTask{
    protected String _name;
    
    private String _region;
    
    private String _endPoint = new String("cloudformation.us-west-2.amazonaws.com");
    
    public void setEndPoint(String endPoint){
        LOG.warn("Setting endpoint at runtime is not threadsafe");
        _endPoint=endPoint;
    }
    
    public void setName(String name){
        _name=name;
    }
    
    public void setRegion(String region){
        _region=region;
        _endPoint = "cloudformation."+ _region +".amazonaws.com";
    }
    
    
    private AmazonCloudFormationAsyncClient createCloudFormation() {
        return new AmazonCloudFormationAsyncClient(
                new BasicAWSCredentials(_accessKey, _accessSecret));
    }
    
    @Override
    public final void execute() throws BuildException {
        validate();
        AmazonCloudFormationAsyncClient client = createCloudFormation();
        client.setEndpoint(_endPoint);
        doExecute(client);
    }

    protected abstract void doExecute(AmazonCloudFormationAsyncClient cloud);

    protected void validate() {
        if (_name == null) throw new BuildException("Stack name must be provided.");
        if (_region == null) throw new BuildException("CloudFormation region must be provided");
    }
}
