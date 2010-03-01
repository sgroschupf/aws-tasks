package datameer.awstasks.aws.emr;

import java.lang.reflect.Method;
import java.util.Map;

import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceException;
import com.amazonaws.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.elasticmapreduce.model.RunJobFlowResponse;

public class AmazonElasticMapReduceCustomClient extends AmazonElasticMapReduceClient {

    private Method _converRunJobFlowMethod;
    private Method _invokeMethod;
    private final Map<String, String> _customRunFlowParameters;

    public AmazonElasticMapReduceCustomClient(String awsAccessKeyId, String awsSecretAccessKey, Map<String, String> customRunFlowParameters) {
        super(awsAccessKeyId, awsSecretAccessKey);
        _customRunFlowParameters = customRunFlowParameters;
        _converRunJobFlowMethod = getMethod("convertRunJobFlow", RunJobFlowRequest.class);
        _invokeMethod = getMethod("invoke", Class.class, Map.class);
    }

    private Method getMethod(String name, Class<?>... paramClasses) {
        try {
            Method method = getClass().getSuperclass().getDeclaredMethod(name, paramClasses);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RunJobFlowResponse runJobFlow(RunJobFlowRequest request) throws AmazonElasticMapReduceException {
        try {
            Map<String, String> parameters = (Map<String, String>) _converRunJobFlowMethod.invoke(this, request);
            System.out.println(parameters);
            System.out.println(_customRunFlowParameters);
            parameters.putAll(_customRunFlowParameters);
            return (RunJobFlowResponse) _invokeMethod.invoke(this, RunJobFlowResponse.class, parameters);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
