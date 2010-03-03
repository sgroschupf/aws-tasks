/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            parameters.putAll(_customRunFlowParameters);
            return (RunJobFlowResponse) _invokeMethod.invoke(this, RunJobFlowResponse.class, parameters);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
