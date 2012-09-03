package palominoz.awstasks.ant.cloudformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.tools.ant.BuildException;

import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.EstimateTemplateCostRequest;
import com.amazonaws.services.cloudformation.model.EstimateTemplateCostResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;
import com.amazonaws.services.cloudformation.model.ValidateTemplateResult;

import datameer.awstasks.util.CloudFormationUtil;
import datameer.awstasks.util.IoUtil;

public abstract class AbstractCloudFormationTemplateTask extends AbstractCloudFormationTask {
    
    public static class Capability{
        private String _value;
        
        public void setValue(String value){
            _value=value;
        }
        
        public String value(){ return _value;}
        
    };
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<Parameter> parameters = new ArrayList();
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<Capability> capabilities = new ArrayList();
    
    private String _template;
    
    private String _templateFile;
    
    
    public void addParameter(Parameter parameter){
        parameters.add(parameter);
    }
    
    public void addCapability(Capability capability){
        capabilities.add(capability);
    }
    
    public void setTemplate(String template){
        _template = template;
    }
    
    public void setTemplateFile(String templateFile){
        _templateFile=templateFile;
    }
    
    
    public void estimate(AmazonCloudFormationAsyncClient cloud){
        try{
            Future<EstimateTemplateCostResult> asyncJob0 = 
                    cloud.estimateTemplateCostAsync(
                            new EstimateTemplateCostRequest().withParameters(parameters).withTemplateBody(_template));
            
            
            EstimateTemplateCostResult result0 = asyncJob0.get();
            
            LOG.info("Please visit\n\t " + result0.getUrl() + "\nto view an estimate monthly cost for this template. ");
        }
        catch(Exception e){
            LOG.error("There was an error estimating your template.");
        }
    }
    
    @SuppressWarnings("rawtypes")
    public List<String> capabilities(){
        @SuppressWarnings("unchecked")
        List<String> strCapabilities = new ArrayList();
        
        for (Capability capability : capabilities){
            strCapabilities.add(capability.value());
        }
        
        return strCapabilities;
    }
    
    
    public void launch(AmazonCloudFormationAsyncClient cloud, String stackName){
        
        try {
            
            if (_templateFile != null){
                _template = IoUtil.readFile(_templateFile);
            }
            validateTemplate(cloud);
            Stack stack = CloudFormationUtil.stackExists(cloud, stackName);
            if (stack != null){
                LOG.info("Stack "+ stackName + " already exist. If you wanted to update the template, please use the Update task. Use the Describe task to obtain stack outputs.");
                return;
            }
            
            
            
            
            CreateStackRequest request= 
                    new CreateStackRequest().withStackName(stackName).withTemplateBody(_template).withParameters(parameters).withCapabilities(capabilities());
            
            Future<CreateStackResult> asyncJob = cloud.createStackAsync(request);
            
            
            CreateStackResult result = asyncJob.get();
            
            if (asyncJob.isDone()){                
                LOG.info("Stack creation was successfully started. Waiting for resources to be ready.."+ stackName);
                waitForStackToBeReady(cloud, stackName);
            }
            else{
                LOG.error("There were issues creating the stack.");
            }
            
        } catch (InterruptedException e) {
            LOG.error("There was an error launching your stack:\n"+e.getMessage());
        } catch (ExecutionException e) {
            LOG.error("There was an error launching your stack:\n"+e.getMessage());
        } catch (IOException e) {
            LOG.error("There was an error with the template:\n"+e.getMessage());
        }
    } 
    
    
    public void waitForStackToBeReady(AmazonCloudFormationAsyncClient cloud, String stackName) throws InterruptedException{
        boolean ready = true;
        do{
            ready = true;
            Stack stack = CloudFormationDescribeStackTask.stackFromStackName(cloud, stackName);
            ready = stack.getStackStatus().equals("CREATE_COMPLETE");
            if (!ready) {
                CloudFormationDescribeStackTask.printStackResourcesStatuses(cloud, stackName);
                LOG.info("Stack is being processed. Sleeping for a while..\n");
                Thread.sleep(10000);
            } 
            else {
                if (stack.getStackStatus().equals("CREATE_COMPLETE")){
                    LOG.info("Stack was successfully launched");
                }
                
                
            }
        }
        while(!ready);
    }
    
    public void update(AmazonCloudFormationAsyncClient cloud, String stackName){
        try {
            Future<UpdateStackResult> asyncJob = cloud.updateStackAsync(new UpdateStackRequest().withStackName(stackName).withCapabilities(capabilities()).withParameters(parameters));
            asyncJob.get();
            waitForStackToBeReady(cloud, stackName);
        } catch (Exception e) {
            LOG.error("There was an error updating the stack:\n"+e.getMessage());
        }
    }
    
    private void validateTemplate(AmazonCloudFormationAsyncClient cloud) throws InterruptedException, ExecutionException{
        ValidateTemplateRequest request = new ValidateTemplateRequest().withTemplateBody(_template);
        Future<ValidateTemplateResult> asyncJob = cloud.validateTemplateAsync(request);
        ValidateTemplateResult result = asyncJob.get();
        LOG.info(result.getCapabilitiesReason());
    }
    
    protected void validate(){
        super.validate();
        if (!(_templateFile!=null ^ _template!=null)) throw new BuildException("Exactly one of template or templateFile must be provided");
    }

}
