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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsResult;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.TerminateJobFlowsRequest;
import datameer.com.google.common.collect.Lists;
import datameer.com.google.common.collect.Maps;

import datameer.awstasks.aws.emr.EmrCluster.InterruptedRuntimeException;
import datameer.awstasks.util.ExceptionUtil;

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
    private long _requestInterval = 10000;
    private int _maxRetriesOnConnectionErrors = 25;
    private JobFlowDescriptionCache _flowDescriptionCache = new JobFlowDescriptionCache(_requestInterval);

    public AmazonElasticMapReduceCustomClient(String awsAccessKeyId, String awsSecretAccessKey) {
        super(new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey));
    }

    public void setRequestInterval(long requestInterval) {
        _requestInterval = requestInterval;
        _flowDescriptionCache.setMaxCacheTime(requestInterval);
    }

    public long getRequestInterval() {
        return _requestInterval;
    }

    public void setMaxRetriesOnConnectionErrors(int maxRetriesOnConnectionErrors) {
        _maxRetriesOnConnectionErrors = maxRetriesOnConnectionErrors;
    }

    public int getMaxRetriesOnConnectionErrors() {
        return _maxRetriesOnConnectionErrors;
    }

    @Override
    public void addJobFlowSteps(final AddJobFlowStepsRequest request) throws AmazonServiceException {
        doThrottleSafe(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AmazonElasticMapReduceCustomClient.super.addJobFlowSteps(request);
                return null;
            }
        });
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
            });
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
        });
    }

    @Override
    public void terminateJobFlows(final TerminateJobFlowsRequest request) throws AmazonServiceException {
        doThrottleSafe(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                AmazonElasticMapReduceCustomClient.super.terminateJobFlows(request);
                return null;
            }
        });
    }

    protected <T> T doThrottleSafe(Callable<T> callable) throws AmazonServiceException, InterruptedRuntimeException {
        int failCount = 0;
        do {
            try {
                return callable.call();
            } catch (AmazonClientException e) {
                failCount++;
                if (!shouldRetryWebServiceCall(e, failCount)) {
                    throw e;
                }
                LOG.warn("retrying after exception: " + e.getMessage());
                try {
                    Thread.sleep(getRequestInterval());
                } catch (InterruptedException e2) {
                    throw new InterruptedRuntimeException(e2);
                }
            } catch (InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            } catch (Exception e) {
                throw ExceptionUtil.convertToRuntimeException(e);
            }

        } while (true);
    }

    private boolean shouldRetryWebServiceCall(AmazonClientException e, int failCount) {
        if (e instanceof AmazonServiceException) {
            String errorCode = ((AmazonServiceException) e).getErrorCode();
            if (errorCode != null && errorCode.equals("Throttling")) {
                return true;
            }
        }
        if (failCount < getMaxRetriesOnConnectionErrors() && e.getMessage() != null) {
            if (e.getMessage().contains("Unable to execute HTTP request")) {
                return true;
            }
            if (e.getMessage().contains("Unable to unmarshall response")) {
                return true;
            }
        }

        return false;
    }

    static class JobFlowDescriptionCache {

        private final Map<String, DescribeJobFlowsResult> _cachedJobFlowsDescriptionsByRequestHash = Maps.newHashMap();
        private final Map<String, Long> _lastRetrievalTimeByRequestHash = Maps.newHashMap();
        private long _maxCacheTime;

        public JobFlowDescriptionCache(long maxCacheTime) {
            _maxCacheTime = maxCacheTime;
        }

        public void setMaxCacheTime(long maxCacheTime) {
            _maxCacheTime = maxCacheTime;
        }

        public void addResponse(DescribeJobFlowsRequest request, DescribeJobFlowsResult response) {
            String hashCode = calculateHashString(request);
            _cachedJobFlowsDescriptionsByRequestHash.put(hashCode, response);
            _lastRetrievalTimeByRequestHash.put(hashCode, System.currentTimeMillis());
        }

        public DescribeJobFlowsResult getResponse(DescribeJobFlowsRequest request) {
            String hash = calculateHashString(request);
            cleanupOutdatedResponses();
            DescribeJobFlowsResult describeJobFlowsResult = _cachedJobFlowsDescriptionsByRequestHash.get(hash);
            if (LOG.isDebugEnabled()) {
                if (describeJobFlowsResult != null) {
                    LOG.debug("Cache hit for: " + hash);
                } else {
                    LOG.debug("Cache miss for: " + hash);
                }
            }
            return describeJobFlowsResult;
        }

        public void clear() {
            _cachedJobFlowsDescriptionsByRequestHash.clear();
            _lastRetrievalTimeByRequestHash.clear();
        }

        private void cleanupOutdatedResponses() {
            List<String> hashsToRemove = Lists.newArrayList();
            for (Entry<String, Long> entry : _lastRetrievalTimeByRequestHash.entrySet()) {
                Long retrievalTime = entry.getValue();
                if (_maxCacheTime <= (System.currentTimeMillis() - retrievalTime)) {
                    hashsToRemove.add(entry.getKey());
                }
            }
            for (String hash : hashsToRemove) {
                _lastRetrievalTimeByRequestHash.remove(hash);
                _cachedJobFlowsDescriptionsByRequestHash.remove(hash);
            }
        }

        private String calculateHashString(DescribeJobFlowsRequest request) {
            StringBuilder hashBuilder = new StringBuilder();
            hashBuilder.append("createdAfter=").append(request.getCreatedAfter());
            hashBuilder.append(", createdBefore=").append(request.getCreatedBefore());
            hashBuilder.append(", jobFlowIds=").append(request.getJobFlowIds());
            hashBuilder.append(", jobFlowStates=").append(request.getJobFlowStates());
            return hashBuilder.toString();
        }

    }

}
