package palominoz.awstasks.ant.cloudformation;

import java.util.List;
import java.util.concurrent.Future;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;

public abstract class CloudFormationDescribeStackTask extends AbstractCloudFormationTask {
    
    private Stack _stack = null;
    
    public static void printStackResourcesStatuses(AmazonCloudFormationAsyncClient cloud, String stackName){
        List<StackResource> resources = resourcesForStack(cloud, stackName);
        for (StackResource resource : resources){
            LOG.info(resource.getResourceType()+"-->"+resource.getResourceStatus());
        }
        
    }
    
    
    public static Stack stackFromStackName(AmazonCloudFormationAsyncClient cloud, String stackName){
        try {
            Future<DescribeStacksResult> job = cloud.describeStacksAsync(new DescribeStacksRequest().withStackName(stackName));
            DescribeStacksResult status;
            status = job.get();
            return status.getStacks().get(0);
        } catch (Exception e) {
            LOG.error("There was an error fetching the stack"+stackName+"\n"+e.getMessage());
        }
        return null;
    }
    
    
    public Stack stack(AmazonCloudFormationAsyncClient cloud){
        if (_stack == null) _stack = stackFromStackName(cloud, _name);
        return _stack;
    }
    
    public static List<StackResource> resourcesForStack(AmazonCloudFormationAsyncClient cloud, String stackName){
        try {
            DescribeStackResourcesResult stackResources;
            Future<DescribeStackResourcesResult> job2 = cloud.describeStackResourcesAsync(new DescribeStackResourcesRequest().withStackName(stackName));
            stackResources = job2.get();
            return stackResources.getStackResources();
        } catch (Exception e) {
            LOG.error("There was an error fetching the resources for the stack"+stackName+"\n"+e.getMessage());
        }
        return null;
    }
    
    
}
