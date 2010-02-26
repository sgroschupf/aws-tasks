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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import com.amazonaws.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceException;
import com.amazonaws.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.elasticmapreduce.model.DescribeJobFlowsResponse;
import com.amazonaws.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.elasticmapreduce.model.JobFlowDetail;
import com.amazonaws.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.elasticmapreduce.model.PlacementType;
import com.amazonaws.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.elasticmapreduce.model.RunJobFlowResponse;
import com.amazonaws.elasticmapreduce.model.StepConfig;
import com.amazonaws.elasticmapreduce.model.StepDetail;
import com.amazonaws.elasticmapreduce.model.TerminateJobFlowsRequest;

import datameer.awstasks.util.IoUtil;

/**
 * Allows access and management of amazons elastic map-reduce. One emr cluster maps to one job flow.
 */
public class EmrCluster {

    private static final StepConfig DEBUG_STEP = new StepConfig("Setup Hadoop Debugging", "TERMINATE_JOB_FLOW", new HadoopJarStepConfig(null,
            "s3://us-east-1.elasticmapreduce/libs/script-runner/script-runner.jar", null, Arrays.asList("s3://us-east-1.elasticmapreduce/libs/state-pusher/0.1/fetch")));

    protected static final Logger LOG = Logger.getLogger(EmrCluster.class);

    private final String _accessKey;
    private final String _accessSecret;
    private final String _bucket;
    private final AmazonElasticMapReduce _emrService;
    private final S3Service _s3Service;

    private String _clusterName = "elastic-cluster";
    private String _s3LogPath = "/emr/logs";
    private String _s3JobJarBasePath = "/emr/jobjars";
    private int _requestInterval = 10000;
    private boolean _debugEnabled = true;
    private String _jobFlowId;

    public EmrCluster(String accessKey, String accessSecret, String bucket) throws S3ServiceException {
        _accessKey = accessKey;
        _accessSecret = accessSecret;
        _bucket = bucket;
        _emrService = new AmazonElasticMapReduceClient(_accessKey, _accessSecret);
        _s3Service = new RestS3Service(new AWSCredentials(_accessKey, _accessSecret));
    }

    public void setName(String clusterName) {
        _clusterName = clusterName;
    }

    public String getName() {
        return _clusterName;
    }

    public void setS3LogPath(String s3LogPath) {
        _s3LogPath = s3LogPath;
    }

    public String getS3LogPath() {
        return _s3LogPath;
    }

    public void setS3JobJarBasePath(String s3JobJarBasePath) {
        _s3JobJarBasePath = s3JobJarBasePath;
    }

    public String getS3JobJarBasePath() {
        return _s3JobJarBasePath;
    }

    public void setRequestInterval(int requestInterval) {
        _requestInterval = requestInterval;
    }

    public int getRequestInterval() {
        return _requestInterval;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        _debugEnabled = debugEnabled;
    }

    public boolean isDebugEnabled() {
        return _debugEnabled;
    }

    public void startup(int instanceCount, String privateKeyName) throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(false);
        LOG.info("starting elastic cluster (job flow) '" + _clusterName + "' ...");
        if (!getRunningJobFlowDetailsByName(_clusterName).isEmpty()) {
            throw new IllegalStateException("cluster/jobFlow with name '" + _clusterName + "' already running");
        }
        boolean keepAlive = true;
        // TODO configure instance sizes
        JobFlowInstancesConfig jobConfig = new JobFlowInstancesConfig("m1.small", "m1.small", instanceCount, privateKeyName, new PlacementType(), keepAlive);
        RunJobFlowRequest startRequest = new RunJobFlowRequest();
        startRequest.setLogUri("s3n://" + _bucket + _s3LogPath);
        startRequest.setInstances(jobConfig);
        startRequest.setName(_clusterName);
        if (isDebugEnabled()) {
            startRequest.withSteps(DEBUG_STEP);
        }
        RunJobFlowResponse startResponse = _emrService.runJobFlow(startRequest);
        _jobFlowId = startResponse.getRunJobFlowResult().getJobFlowId();
        waitUntilClusterStarted(_jobFlowId);
        LOG.info("elastic cluster '" + _clusterName + "/" + _jobFlowId + "' started");
    }

    public void connect() throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(false);
        List<JobFlowDetail> jobFlows = getRunningJobFlowDetailsByName(_clusterName);
        if (jobFlows.isEmpty()) {
            throw new IllegalStateException("no cluster/jobFlow with name '" + _clusterName + "' running");
        }
        if (jobFlows.size() > 1) {
            throw new IllegalStateException("more then one cluster/jobFlow with name '" + _clusterName + "' running");
        }
        _jobFlowId = jobFlows.get(0).getJobFlowId();
        waitUntilClusterStarted(_jobFlowId);
    }

    /**
     * Connect to a cluster/jobFlow with the given name.
     * 
     * @param jobFlowId
     * @throws InterruptedException
     * @throws AmazonElasticMapReduceException
     */
    public void connect(String jobFlowId) throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(false);
        _jobFlowId = jobFlowId;
        waitUntilClusterStarted(_jobFlowId);
    }

    public void shutdown() throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(true);
        _emrService.terminateJobFlows(new TerminateJobFlowsRequest().withJobFlowIds(_jobFlowId));
        waitUntilClusterShutdown(_jobFlowId);
        _jobFlowId = null;
    }

    public boolean isConnected() {
        return _jobFlowId != null;
    }

    public String getJobFlowId() {
        return _jobFlowId;
    }

    protected void checkConnection(boolean shouldRun) {
        if (shouldRun && !isConnected()) {
            throw new IllegalStateException("not connected to cluster/jobFlow");
        }
        if (!shouldRun && isConnected()) {
            throw new IllegalStateException("already connected to cluster/jobFlow");
        }
    }

    public void executeJobStep(String name, File jobJar, String... args) throws InterruptedException, IOException, AmazonElasticMapReduceException, S3ServiceException {
        executeJobStep(name, jobJar, null, args);
    }

    public void executeJobStep(String name, File jobJar, Class<?> mainClass, String... args) throws InterruptedException, IOException, AmazonElasticMapReduceException, S3ServiceException {
        executeJobStep(name, jobJar, jobJar.getName(), mainClass, args);
    }

    public void executeJobStep(String name, File jobJar, String s3JobJarName, Class<?> mainClass, String... args) throws InterruptedException, IOException, AmazonElasticMapReduceException,
            S3ServiceException {
        String s3JobJarUri = uploadingJobJar(jobJar, s3JobJarName);
        HadoopJarStepConfig jarConfig = new HadoopJarStepConfig(null, s3JobJarUri, mainClass == null ? null : mainClass.getName(), Arrays.asList(args));
        StepConfig stepConfig = new StepConfig(name, "CONTINUE", jarConfig);
        _emrService.addJobFlowSteps(new AddJobFlowStepsRequest().withJobFlowId(_jobFlowId).withSteps(stepConfig));
        waitUntilStepFinished(_jobFlowId, stepConfig);
    }

    private String uploadingJobJar(File jobJar, String s3JobJarName) throws S3ServiceException, IOException {
        String s3JobJarPath = new File(_s3JobJarBasePath, s3JobJarName).getPath();
        S3Bucket bucket = _s3Service.getBucket(_bucket);
        if (!exists(s3JobJarPath, bucket)) {
            LOG.info("uploading " + jobJar + " to " + s3JobJarPath);
            IoUtil.uploadFile(_s3Service, _bucket, jobJar, s3JobJarPath);
        }
        return "s3n://" + _accessKey + "@" + _bucket + s3JobJarPath;
    }

    private boolean exists(String s3JobJarPath, S3Bucket bucket) throws S3ServiceException {
        S3Object[] s3Objects = _s3Service.listObjects(bucket, s3JobJarPath, null);
        for (S3Object s3Object : s3Objects) {
            if (s3Object.getKey().equals(s3JobJarPath)) {
                return true;
            }
        }
        return false;
    }

    private void waitUntilClusterStarted(final String jobFlowId) throws AmazonElasticMapReduceException, InterruptedException {
        waitUntil(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobFlowDetail jobFlowDetail = getJobFlowDetail(jobFlowId);
                JobFlowState state = JobFlowState.valueOf(jobFlowDetail.getExecutionStatusDetail().getState());
                LOG.info("elastic cluster '" + jobFlowId + "' in state '" + state + "'");
                boolean finished = state != JobFlowState.STARTING;
                if (finished) {
                    if (!state.isOperational()) {
                        throw new IllegalStateException("starting of job flow '" + jobFlowId + "' failed with state '" + state + "'");
                    }
                }
                return finished;
            }
        }, _requestInterval);
    }

    private void waitUntilClusterShutdown(final String jobFlowId) throws InterruptedException, AmazonElasticMapReduceException {
        waitUntil(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobFlowDetail jobFlowDetail = getJobFlowDetail(jobFlowId);
                JobFlowState state = JobFlowState.valueOf(jobFlowDetail.getExecutionStatusDetail().getState());
                LOG.info("elastic cluster '" + jobFlowId + "' in state '" + state + "'");
                boolean finished = state != JobFlowState.SHUTTING_DOWN;
                if (finished && state.isOperational()) {
                    throw new IllegalStateException("stopping of job flow '" + jobFlowId + "' failed with state '" + state + "'");
                }
                return finished;
            }
        }, _requestInterval);
    }

    private void waitUntilStepFinished(final String jobFlowId, final StepConfig stepConfig) throws InterruptedException, AmazonElasticMapReduceException {
        waitUntil(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobFlowDetail flowDetail = getJobFlowDetail(jobFlowId);
                StepDetail stepDetail = getStepDetail(stepConfig, flowDetail);
                StepState stepState = StepState.valueOf(stepDetail.getExecutionStatusDetail().getState());
                LOG.info("job step '" + stepConfig.getName() + "' in state '" + stepState + "'");
                boolean finished = stepState.isFinished();
                if (finished) {
                    if (!stepState.isSuccessful()) {
                        throw new RuntimeException("job step '" + stepConfig.getName() + "' failed (state: " + stepState + ")");
                    }
                }
                return finished;
            }
        }, _requestInterval);
    }

    protected static void waitUntil(Callable<Boolean> callable, int requestInterval) throws AmazonElasticMapReduceException, InterruptedException {
        boolean finished = false;
        do {
            try {
                finished = callable.call();
            } catch (AmazonElasticMapReduceException e) {
                String errorCode = e.getErrorCode();
                if (errorCode == null || !errorCode.equals("Throttling")) {
                    throw e;
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!finished) {
                Thread.sleep(requestInterval);
            }
        } while (!finished);
    }

    protected JobFlowDetail getJobFlowDetail(String jobFlowId) throws AmazonElasticMapReduceException {
        DescribeJobFlowsResponse describeJobFlows = _emrService.describeJobFlows(new DescribeJobFlowsRequest().withJobFlowIds(jobFlowId));
        List<JobFlowDetail> jobFlows = describeJobFlows.getDescribeJobFlowsResult().getJobFlows();
        if (jobFlows.isEmpty()) {
            throw new IllegalArgumentException("no job flow with id '" + _jobFlowId + "' found");
        }
        return jobFlows.get(0);
    }

    protected List<JobFlowDetail> getRunningJobFlowDetailsByName(String name) throws AmazonElasticMapReduceException {
        DescribeJobFlowsResponse describeJobFlows = _emrService.describeJobFlows(new DescribeJobFlowsRequest().withJobFlowStates(JobFlowState.STARTING.name(), JobFlowState.WAITING.name(),
                JobFlowState.RUNNING.name()));
        List<JobFlowDetail> jobFlows = describeJobFlows.getDescribeJobFlowsResult().getJobFlows();
        for (Iterator iterator = jobFlows.iterator(); iterator.hasNext();) {
            JobFlowDetail jobFlowDetail = (JobFlowDetail) iterator.next();
            if (!name.equals(jobFlowDetail.getName())) {
                iterator.remove();
            }
        }
        return jobFlows;
    }

    protected StepDetail getStepDetail(StepConfig stepConfig, JobFlowDetail flowDetail) {
        for (StepDetail stepDetail : flowDetail.getSteps()) {
            if (stepConfig.getName().equals(stepDetail.getStepConfig().getName())) {
                return stepDetail;
            }
        }
        throw new IllegalStateException("no step detail with name '" + stepConfig.getName() + "' found in " + flowDetail.getJobFlowId());
    }

}
