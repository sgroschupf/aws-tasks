package palominoz.awstasks.ant.cloudformation;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;


public class CloudFormationCreateStackTask extends AbstractCloudFormationTemplateTask {
    
    @Override
    protected void doExecute(AmazonCloudFormationAsyncClient cloud) { 
       launch(cloud, _name);
    }

}
