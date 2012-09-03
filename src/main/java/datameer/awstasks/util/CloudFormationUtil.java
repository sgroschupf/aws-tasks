package datameer.awstasks.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;

public class CloudFormationUtil {
    
    public static Stack stackExists(AmazonCloudFormationAsyncClient cloud, String stackName){
        
        try {
            Future<DescribeStacksResult> job = cloud.describeStacksAsync(new DescribeStacksRequest().withStackName(stackName));
            DescribeStacksResult status;
            status = job.get();
            if (!status.getStacks().isEmpty()) return status.getStacks().get(0);
            else return null;
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

}
