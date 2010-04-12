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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceException;
import com.amazonaws.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.elasticmapreduce.model.AddJobFlowStepsResponse;
import com.amazonaws.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.elasticmapreduce.model.DescribeJobFlowsResponse;
import com.amazonaws.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.elasticmapreduce.model.RunJobFlowResponse;
import com.amazonaws.elasticmapreduce.model.TerminateJobFlowsRequest;
import com.amazonaws.elasticmapreduce.model.TerminateJobFlowsResponse;

import datameer.awstasks.aws.emr.EmrCluster.InterruptedRuntimeException;

/**
 * 
 * A custom {@link AmazonElasticMapReduceClient} which adds following features:<br>
 * - custom flow run parameters<br>
 * - throttle safe invocation of web service methods<br>
 * - caching of flow descriptions (to avoid throttle exception flood with multiple running flow
 * steps)<br>
 * 
 */
@SuppressWarnings("synthetic-access")
public class AmazonElasticMapReduceCustomClient extends AmazonElasticMapReduceClient {

    protected static final Logger LOG = Logger.getLogger(AmazonElasticMapReduceCustomClient.class);
    private final Method _converRunJobFlowMethod;
    private final Method _invokeMethod;
    private final Map<String, String> _customRunFlowParameters;
    private int _requestInterval = 10000;
    private JobFlowDescriptionCache _flowDescriptionCache = new JobFlowDescriptionCache(_requestInterval);

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

    public void setRequestInterval(int requestInterval) {
        _requestInterval = requestInterval;
        _flowDescriptionCache.setMaxCacheTime(requestInterval);
    }

    public int getRequestInterval() {
        return _requestInterval;
    }

    @Override
    public AddJobFlowStepsResponse addJobFlowSteps(final AddJobFlowStepsRequest request) throws AmazonElasticMapReduceException {
        return doThrottleSafe(new Callable<AddJobFlowStepsResponse>() {
            @Override
            public AddJobFlowStepsResponse call() throws Exception {
                return AmazonElasticMapReduceCustomClient.super.addJobFlowSteps(request);
            }
        }, getRequestInterval());
    }

    @Override
    public synchronized DescribeJobFlowsResponse describeJobFlows(final DescribeJobFlowsRequest request) throws AmazonElasticMapReduceException {
        DescribeJobFlowsResponse cachedResponse = _flowDescriptionCache.getResponse(request);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        return doThrottleSafe(new Callable<DescribeJobFlowsResponse>() {
            @Override
            public DescribeJobFlowsResponse call() throws Exception {
                DescribeJobFlowsResponse response = AmazonElasticMapReduceCustomClient.super.describeJobFlows(request);
                _flowDescriptionCache.addResponse(request, response);
                return response;
            }
        }, getRequestInterval());
    }

    @Override
    public RunJobFlowResponse runJobFlow(final RunJobFlowRequest request) throws AmazonElasticMapReduceException {
        return doThrottleSafe(new Callable<RunJobFlowResponse>() {
            @Override
            public RunJobFlowResponse call() throws Exception {
                Map<String, String> parameters = (Map<String, String>) _converRunJobFlowMethod.invoke(AmazonElasticMapReduceCustomClient.this, request);
                parameters.putAll(_customRunFlowParameters);
                return (RunJobFlowResponse) _invokeMethod.invoke(AmazonElasticMapReduceCustomClient.this, RunJobFlowResponse.class, parameters);
            }
        }, getRequestInterval());
    }

    @Override
    public TerminateJobFlowsResponse terminateJobFlows(final TerminateJobFlowsRequest request) throws AmazonElasticMapReduceException {
        return doThrottleSafe(new Callable<TerminateJobFlowsResponse>() {
            @Override
            public TerminateJobFlowsResponse call() throws Exception {
                return AmazonElasticMapReduceCustomClient.super.terminateJobFlows(request);
            }
        }, getRequestInterval());
    }

    protected static <T> T doThrottleSafe(Callable<T> callable, int requestInterval) throws AmazonElasticMapReduceException, InterruptedRuntimeException {
        T result;
        try {
            result = callable.call();
        } catch (AmazonElasticMapReduceException e) {
            String errorCode = e.getErrorCode();
            if (errorCode == null || !errorCode.equals("Throttling")) {
                throw e;
            }
            LOG.warn("throttle exception: " + e.getMessage());
            try {
                Thread.sleep(requestInterval);
            } catch (InterruptedException e2) {
                throw new InterruptedRuntimeException(e2);
            }
            return doThrottleSafe(callable, requestInterval);
        } catch (InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    static class JobFlowDescriptionCache {

        private final Map<Integer, DescribeJobFlowsResponse> _cachedJobFlowsDescriptionsByRequestHash = new HashMap<Integer, DescribeJobFlowsResponse>();
        private final Map<Integer, Long> _lastRetrievalTimeByRequestHash = new HashMap<Integer, Long>();
        private int _maxCacheTime;

        public JobFlowDescriptionCache(int maxCacheTime) {
            _maxCacheTime = maxCacheTime;
        }

        public void setMaxCacheTime(int maxCacheTime) {
            _maxCacheTime = maxCacheTime;
        }

        public void addResponse(DescribeJobFlowsRequest request, DescribeJobFlowsResponse response) {
            int hashCode = calculateHashCode(request);
            _cachedJobFlowsDescriptionsByRequestHash.put(hashCode, response);
            _lastRetrievalTimeByRequestHash.put(hashCode, System.currentTimeMillis());
        }

        public DescribeJobFlowsResponse getResponse(DescribeJobFlowsRequest request) {
            int hashCode = calculateHashCode(request);
            cleanupOutdatedResponses();
            return _cachedJobFlowsDescriptionsByRequestHash.get(hashCode);
        }

        private void cleanupOutdatedResponses() {
            List<Integer> hashsToRemove = new ArrayList<Integer>(3);
            for (Entry<Integer, Long> entry : _lastRetrievalTimeByRequestHash.entrySet()) {
                Long retrievalTime = entry.getValue();
                if (_maxCacheTime <= (System.currentTimeMillis() - retrievalTime)) {
                    hashsToRemove.add(entry.getKey());
                }
            }
            for (Integer hash : hashsToRemove) {
                _lastRetrievalTimeByRequestHash.remove(hash);
                _cachedJobFlowsDescriptionsByRequestHash.remove(hash);
            }
        }

        private int calculateHashCode(DescribeJobFlowsRequest request) {
            StringBuilder hashBuilder = new StringBuilder();
            addIfNotNull(hashBuilder, request.getCreatedAfter());
            addIfNotNull(hashBuilder, request.getCreatedBefore());
            addIfNotNull(hashBuilder, request.getJobFlowIds().toString());
            addIfNotNull(hashBuilder, request.getJobFlowStates().toString());
            return hashBuilder.toString().hashCode();
        }

        private void addIfNotNull(StringBuilder hashBuilder, String string) {
            if (string != null) {
                hashBuilder.append(string);
            }
        }
    }

}
