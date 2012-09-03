package palominoz.awstasks.ant.cloudformation;

import org.apache.tools.ant.BuildException;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.Stack;

import datameer.awstasks.util.CloudFormationUtil;

public class CloudFormationStackExistTask extends AbstractCloudFormationTask {

    
    private String _propertyKey;
    
    public void setPropertyKey(String propertyKey){
        _propertyKey=propertyKey;
    }
    
    @Override
    protected void doExecute(AmazonCloudFormationAsyncClient cloud) {
        Stack stack = CloudFormationUtil.stackExists(cloud, _name);
        if (stack!=null) setProperty(_propertyKey, "true");
    }

    protected void validate(){
        super.validate();
        if (_propertyKey == null) throw new BuildException("Property key must be set.");
    }
 }
