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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsResult;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.TerminateJobFlowsRequest;

import datameer.awstasks.aws.emr.EmrCluster.InterruptedRuntimeException;

/**
 * 
 * A custom {@link AmazonElasticMapReduceClient} which adds following features:<br>
 * - throttle safe invocation of web service methods<br>
 * - caching of flow descriptions (to avoid throttle exception flood with multiple running flow
 * steps)<br>
 * 
 */
@SuppressWarnings("synthetic-access")
public class AmazonElasticMapReduceCustomClient extends AmazonElasticMapReduceClient {

    protected static final Logger LOG = Logger.getLogger(AmazonElasticMapReduceCustomClient.class);
    private int _requestInterval = 10000;
    private JobFlowDescriptionCache _flowDescriptionCache = new JobFlowDescriptionCache(_requestInterval);

    public AmazonElasticMapReduceCustomClient(String awsAccessKeyId, String awsSecretAccessKey) {
        super(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey));
    }

    public void setRequestInterval(int requestInterval) {
        _requestInterval = requestInterval;
        _flowDescriptionCache.setMaxCacheTime(requestInterval);
    }

    public int getRequestInterval() {
        return _requestInterval;
    }

    @Override
    public void addJobFlowSteps(final AddJobFlowStepsRequest request) throws AmazonServiceException {
        doThrottleSafe(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AmazonElasticMapReduceCustomClient.super.addJobFlowSteps(request);
                return null;
            }
        }, getRequestInterval());
    }

    @Override
    public DescribeJobFlowsResult describeJobFlows(final DescribeJobFlowsRequest request) throws AmazonServiceException {
        synchronized (_flowDescriptionCache) {
            DescribeJobFlowsResult cachedResponse = _flowDescriptionCache.getResponse(request);
            if (cachedResponse != null) {
                return cachedResponse;
            }
            return doThrottleSafe(new Callable<DescribeJobFlowsResult>() {
                @Override
                public DescribeJobFlowsResult call() throws Exception {
                    DescribeJobFlowsResult response = AmazonElasticMapReduceCustomClient.super.describeJobFlows(request);
                    _flowDescriptionCache.addResponse(request, response);
                    return response;
                }
            }, getRequestInterval());
        }
    }

    public void clearDescribeJobFlowCache() {
        synchronized (_flowDescriptionCache) {
            _flowDescriptionCache.clear();
        }
    }

    @Override
    public RunJobFlowResult runJobFlow(final RunJobFlowRequest request) throws AmazonServiceException {
        return doThrottleSafe(new Callable<RunJobFlowResult>() {
            @Override
            public RunJobFlowResult call() throws Exception {
                return AmazonElasticMapReduceCustomClient.super.runJobFlow(request);
            }
        }, getRequestInterval());
    }

    @Override
    public void terminateJobFlows(final TerminateJobFlowsRequest request) throws AmazonServiceException {
        doThrottleSafe(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AmazonElasticMapReduceCustomClient.super.terminateJobFlows(request);
                return null;
            }
        }, getRequestInterval());
    }

    protected static <T> T doThrottleSafe(Callable<T> callable, int requestInterval) throws AmazonServiceException, InterruptedRuntimeException {
        T result;
        try {
            result = callable.call();
        } catch (AmazonServiceException e) {
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

        private final Map<Integer, DescribeJobFlowsResult> _cachedJobFlowsDescriptionsByRequestHash = new HashMap<Integer, DescribeJobFlowsResult>();
        private final Map<Integer, Long> _lastRetrievalTimeByRequestHash = new HashMap<Integer, Long>();
        private int _maxCacheTime;

        public JobFlowDescriptionCache(int maxCacheTime) {
            _maxCacheTime = maxCacheTime;
        }

        public void setMaxCacheTime(int maxCacheTime) {
            _maxCacheTime = maxCacheTime;
        }

        public void addResponse(DescribeJobFlowsRequest request, DescribeJobFlowsResult response) {
            int hashCode = calculateHashCode(request);
            _cachedJobFlowsDescriptionsByRequestHash.put(hashCode, response);
            _lastRetrievalTimeByRequestHash.put(hashCode, System.currentTimeMillis());
        }

        public DescribeJobFlowsResult getResponse(DescribeJobFlowsRequest request) {
            int hashCode = calculateHashCode(request);
            cleanupOutdatedResponses();
            return _cachedJobFlowsDescriptionsByRequestHash.get(hashCode);
        }

        public void clear() {
            _cachedJobFlowsDescriptionsByRequestHash.clear();
            _lastRetrievalTimeByRequestHash.clear();
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
            addIfNotNull(hashBuilder, request.getJobFlowIds());
            addIfNotNull(hashBuilder, request.getJobFlowStates());
            return hashBuilder.toString().hashCode();
        }

        private void addIfNotNull(StringBuilder hashBuilder, Object object) {
            if (object != null) {
                hashBuilder.append(object.toString());
            }
        }
    }

}
