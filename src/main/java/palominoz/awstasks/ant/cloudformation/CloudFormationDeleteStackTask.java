package palominoz.awstasks.ant.cloudformation;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;

public class CloudFormationDeleteStackTask extends AbstractCloudFormationTask {
  
    @Override
    protected void doExecute(AmazonCloudFormationAsyncClient cloud) { 
        try {
            
            DeleteStackRequest request= 
                    new DeleteStackRequest().withStackName(_name);
            
            Future<Void> asyncJob = cloud.deleteStackAsync(request);
            
            asyncJob.get();
            
            if (asyncJob.isDone()){
                LOG.info("Successfully deleted stack "+ _name);
            }
            else{
                LOG.info("There were isssues deleting the stack.");
            }
            
        } catch (InterruptedException e) {
            LOG.info(e.getMessage());
        } catch (ExecutionException e) {
            LOG.info(e.getMessage());
        } 
    }
}
