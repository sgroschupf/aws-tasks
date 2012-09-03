package palominoz.awstasks.ant.cloudformation;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult; 
import com.amazonaws.services.cloudformation.model.Stack;

public class CloudFormationInfoTask extends AbstractCloudFormationTask {

    
    
    public List<Stack> stacksAvailableIn(AmazonCloudFormationAsyncClient cloud){
        try {
            Future<DescribeStacksResult> asyncJob = cloud.describeStacksAsync(new DescribeStacksRequest());
            DescribeStacksResult results =  asyncJob.get();
            if (results.getStacks().size()==0) LOG.info("There were no stacks associated with your account.");
            return results.getStacks();
        } catch (InterruptedException e) {
            LOG.error(e.getMessage());
        } catch (ExecutionException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }   
    
    @Override
    protected void doExecute(AmazonCloudFormationAsyncClient cloud) {
       
        LOG.info("Stacks found for your account:\n ");
        for (Stack stack : stacksAvailableIn(cloud)){
            LOG.info("Stack:\n\tID: " + stack.getStackId() + "\n\tNAME: "+ stack.getStackName());
        }
    }

}
