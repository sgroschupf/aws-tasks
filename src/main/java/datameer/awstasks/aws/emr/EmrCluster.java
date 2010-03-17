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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import com.amazonaws.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.elasticmapreduce.AmazonElasticMapReduceException;
import com.amazonaws.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.elasticmapreduce.model.AddJobFlowStepsResponse;
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
import com.amazonaws.elasticmapreduce.model.TerminateJobFlowsResponse;

import datameer.awstasks.util.IoUtil;

/**
 * Allows access and management of amazons elastic map-reduce. One emr cluster maps to one job flow.
 */
public class EmrCluster {

    private static final StepConfig DEBUG_STEP = new StepConfig("Setup Hadoop Debugging", "TERMINATE_JOB_FLOW", new HadoopJarStepConfig(null,
            "s3://us-east-1.elasticmapreduce/libs/script-runner/script-runner.jar", null, Arrays.asList("s3://us-east-1.elasticmapreduce/libs/state-pusher/0.1/fetch")));

    protected static final Logger LOG = Logger.getLogger(EmrCluster.class);
    protected final static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final EmrSettings _settings;
    private final String _accessSecret;
    protected ThrottleSafeWebServiceClient _emrService;
    private S3Service _s3Service;
    protected long _startTime;
    protected String _masterHost;
    protected int _instanceCount;

    protected String _jobFlowId;

    // TODO jz: rethrow interrupted exceptions

    public EmrCluster(EmrSettings settings, String accessSecret) {
        _accessSecret = accessSecret;
        _settings = settings;
        _emrService = new ThrottleSafeWebServiceClient(new AmazonElasticMapReduceCustomClient(settings.getAccessKey(), _accessSecret, getSettings().getCustomStartParameter()));
        FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public String getName() {
        return getSettings().getClusterName();
    }

    public EmrSettings getSettings() {
        return _settings;
    }

    public AmazonElasticMapReduce getEmrService() {
        return _emrService;
    }

    public void setRequestInterval(int requestInterval) {
        _emrService.setRequestInterval(requestInterval);
    }

    public int getRequestInterval() {
        return _emrService.getRequestInterval();
    }

    public long getStartTime() {
        checkConnection(true);
        return _startTime;
    }

    public String getMasterHost() {
        checkConnection(true);
        return _masterHost;
    }

    public int getInstanceCount() {
        checkConnection(true);
        return _instanceCount;
    }

    public void startup() throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(false);
        EmrSettings settings = getSettings();
        if (settings.getPrivateKeyName() == null) {
            throw new NullPointerException("privateKeyName must not be null please configure settings properly");
        }
        LOG.info("starting elastic cluster (job flow) '" + getName() + "' ...");
        if (!getRunningJobFlowDetailsByName(getName()).isEmpty()) {
            throw new IllegalStateException("cluster/jobFlow with name '" + getName() + "' already running");
        }
        boolean keepAlive = true;
        JobFlowInstancesConfig jobConfig = new JobFlowInstancesConfig(settings.getMasterInstanceType().getId(), settings.getNodeInstanceType().getId(), settings.getInstanceCount(), settings
                .getPrivateKeyName(), new PlacementType(), keepAlive);
        final RunJobFlowRequest startRequest = new RunJobFlowRequest();
        startRequest.setLogUri("s3n://" + settings.getS3Bucket() + settings.getS3LogPath());
        startRequest.setInstances(jobConfig);
        startRequest.setName(getName());
        if (settings.isDebugEnabled()) {
            startRequest.withSteps(DEBUG_STEP);
        }
        RunJobFlowResponse startResponse = _emrService.runJobFlow(startRequest);
        _jobFlowId = startResponse.getRunJobFlowResult().getJobFlowId();
        waitUntilClusterStarted(_jobFlowId);
        LOG.info("elastic cluster '" + getName() + "/" + _jobFlowId + "' started, master-host is " + getJobFlowDetail(_jobFlowId).getInstances().getMasterPublicDnsName());
    }

    /**
     * Connect by cluster name.
     * 
     * @throws InterruptedException
     * @throws AmazonElasticMapReduceException
     */
    public void connectByName() throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(false);
        List<JobFlowDetail> jobFlows = getRunningJobFlowDetailsByName(getName());
        if (jobFlows.isEmpty()) {
            throw new IllegalStateException("no cluster/jobFlow with name '" + getName() + "' running");
        }
        if (jobFlows.size() > 1) {
            throw new IllegalStateException("more then one cluster/jobFlow with name '" + getName() + "' running");
        }
        connectById(jobFlows.get(0).getJobFlowId());
    }

    /**
     * Connect to a cluster/jobFlow with the given id.
     * 
     * @param jobFlowId
     * @throws InterruptedException
     * @throws AmazonElasticMapReduceException
     */
    public void connectById(String jobFlowId) throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(false);
        _jobFlowId = jobFlowId;
        waitUntilClusterStarted(jobFlowId);
        LOG.info("connected to elastic cluster '" + getName() + "/" + _jobFlowId + "', master-host is " + getJobFlowDetail(_jobFlowId).getInstances().getMasterPublicDnsName());
    }

    public void disconnect() {
        checkConnection(true);
        _jobFlowId = null;
        _startTime = 0;
        // shutdownS3Service();
    }

    public void shutdown() throws InterruptedException, AmazonElasticMapReduceException {
        checkConnection(true);
        _emrService.terminateJobFlows(new TerminateJobFlowsRequest().withJobFlowIds(_jobFlowId));
        waitUntilClusterShutdown(_jobFlowId);
        disconnect();
    }

    // private void shutdownS3Service() {
    // jz: not in verion 0.6
    // if (_s3Service != null) {
    // try {
    // _s3Service.shutdown();
    // } catch (S3ServiceException e) {
    // throw new RuntimeException(e);
    // }
    // }
    // }

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

    public StepFuture executeJobStep(String name, File jobJar, String... args) throws IOException, AmazonElasticMapReduceException, S3ServiceException {
        return executeJobStep(name, jobJar, null, args);
    }

    public StepFuture executeJobStep(String name, File jobJar, Class<?> mainClass, String... args) throws IOException, AmazonElasticMapReduceException, S3ServiceException {
        return executeJobStep(name, jobJar, jobJar.getName(), mainClass, args);
    }

    public StepFuture executeJobStep(String name, File jobJar, String s3JobJarName, Class<?> mainClass, String... args) throws IOException, AmazonElasticMapReduceException, S3ServiceException {
        String s3JobJarUri = uploadingJobJar(jobJar, s3JobJarName);
        HadoopJarStepConfig jarConfig = new HadoopJarStepConfig(null, s3JobJarUri, mainClass == null ? null : mainClass.getName(), Arrays.asList(args));
        StepConfig stepConfig = new StepConfig(name, "CONTINUE", jarConfig);
        _emrService.addJobFlowSteps(new AddJobFlowStepsRequest().withJobFlowId(_jobFlowId).withSteps(stepConfig));
        return new StepFuture(stepConfig.getName(), getStepIndex(getJobFlowDetail(_jobFlowId), name));
    }

    private String uploadingJobJar(File jobJar, String s3JobJarName) throws S3ServiceException, IOException {
        if (_s3Service == null) {
            _s3Service = new RestS3Service(new AWSCredentials(getSettings().getAccessKey(), _accessSecret));
        }
        String s3JobJarPath = new File(getSettings().getS3JobJarBasePath(), s3JobJarName).getPath();
        String s3Bucket = getSettings().getS3Bucket();
        if (!IoUtil.existsFile(_s3Service, s3Bucket, s3JobJarPath)) {
            LOG.info("uploading " + jobJar + " to " + s3JobJarPath);
            IoUtil.uploadFile(_s3Service, s3Bucket, jobJar, s3JobJarPath);
        } else {
            LOG.info("using cached job-jar: " + s3JobJarPath);
        }
        return "s3n://" + getSettings().getAccessKey() + "@" + s3Bucket + s3JobJarPath;
    }

    private void waitUntilClusterStarted(final String jobFlowId) throws AmazonElasticMapReduceException, InterruptedException {
        doThrottleSafeWhile(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobFlowDetail jobFlowDetail = getJobFlowDetail(jobFlowId);
                JobFlowState state = JobFlowState.valueOf(jobFlowDetail.getExecutionStatusDetail().getState());
                LOG.info("elastic cluster '" + jobFlowDetail.getName() + "/" + jobFlowId + "' in state '" + state + "'");
                boolean finished = state != JobFlowState.STARTING;
                if (finished) {
                    _masterHost = jobFlowDetail.getInstances().getMasterPublicDnsName();
                    _instanceCount = jobFlowDetail.getInstances().getInstanceCount();
                    if (!state.isOperational()) {
                        throw new IllegalStateException("starting of job flow '" + jobFlowId + "' failed with state '" + state + "'");
                    }
                    String startDateTime = jobFlowDetail.getExecutionStatusDetail().getStartDateTime();
                    try {
                        _startTime = FORMAT.parse(startDateTime).getTime();
                    } catch (ParseException e) {
                        throw new RuntimeException("could not parse '" + startDateTime + "' with '" + FORMAT.toPattern() + "'", e);
                    }

                }
                return finished;
            }
        }, getRequestInterval());
    }

    private void waitUntilClusterShutdown(final String jobFlowId) throws InterruptedException, AmazonElasticMapReduceException {
        doThrottleSafeWhile(new Callable<Boolean>() {
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
        }, getRequestInterval());
    }

    protected void waitUntilStepFinished(final String jobFlowId, final String stepName) throws InterruptedException, AmazonElasticMapReduceException {
        doThrottleSafeWhile(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobFlowDetail flowDetail = getJobFlowDetail(jobFlowId);
                StepDetail stepDetail = getStepDetail(flowDetail, stepName);
                StepState stepState = StepState.valueOf(stepDetail.getExecutionStatusDetail().getState());
                LOG.info("job step '" + stepName + "' in state '" + stepState + "'");
                boolean finished = stepState.isFinished();
                if (finished) {
                    if (!stepState.isSuccessful()) {
                        throw new RuntimeException("job step '" + stepName + "' failed (state: " + stepState + ")");
                    }
                }
                return finished;
            }

        }, getRequestInterval());
    }

    protected static void doThrottleSafeWhile(Callable<Boolean> callable, int requestInterval) throws AmazonElasticMapReduceException, InterruptedException {
        boolean finished = false;
        do {
            finished = ThrottleSafeWebServiceClient.doThrottleSafe(callable, requestInterval);
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

    protected StepDetail getStepDetail(JobFlowDetail flowDetail, String stepName) {
        for (StepDetail stepDetail : flowDetail.getSteps()) {
            if (stepName.equals(stepDetail.getStepConfig().getName())) {
                return stepDetail;
            }
        }
        throw new IllegalStateException("no step detail with name '" + stepName + "' found in " + flowDetail.getJobFlowId());
    }

    protected int getStepIndex(JobFlowDetail flowDetail, String stepName) {
        for (int i = 0; i < flowDetail.getSteps().size(); i++) {
            if (stepName.equals(flowDetail.getSteps().get(i).getStepConfig().getName())) {
                return i + 1;// starting from 1
            }
        }
        throw new IllegalStateException("no step detail with name '" + stepName + "' found in " + flowDetail.getJobFlowId());
    }

    /**
     * TODO move to {@link AmazonElasticMapReduceCustomClient} ??
     */
    static class ThrottleSafeWebServiceClient implements AmazonElasticMapReduce {

        private int _requestInterval = 10000;
        protected final AmazonElasticMapReduce _delegateClient;

        public ThrottleSafeWebServiceClient(AmazonElasticMapReduce delegateClient) {
            _delegateClient = delegateClient;
        }

        public void setRequestInterval(int requestInterval) {
            _requestInterval = requestInterval;
        }

        public int getRequestInterval() {
            return _requestInterval;
        }

        @Override
        public AddJobFlowStepsResponse addJobFlowSteps(final AddJobFlowStepsRequest request) throws AmazonElasticMapReduceException {
            return doThrottleSafe(new Callable<AddJobFlowStepsResponse>() {
                @Override
                public AddJobFlowStepsResponse call() throws Exception {
                    return _delegateClient.addJobFlowSteps(request);
                }
            }, getRequestInterval());
        }

        @Override
        public DescribeJobFlowsResponse describeJobFlows(final DescribeJobFlowsRequest request) throws AmazonElasticMapReduceException {
            return doThrottleSafe(new Callable<DescribeJobFlowsResponse>() {
                @Override
                public DescribeJobFlowsResponse call() throws Exception {
                    return _delegateClient.describeJobFlows(request);
                }
            }, getRequestInterval());
        }

        @Override
        public RunJobFlowResponse runJobFlow(final RunJobFlowRequest request) throws AmazonElasticMapReduceException {
            return doThrottleSafe(new Callable<RunJobFlowResponse>() {
                @Override
                public RunJobFlowResponse call() throws Exception {
                    return _delegateClient.runJobFlow(request);
                }
            }, getRequestInterval());
        }

        @Override
        public TerminateJobFlowsResponse terminateJobFlows(final TerminateJobFlowsRequest request) throws AmazonElasticMapReduceException {
            return doThrottleSafe(new Callable<TerminateJobFlowsResponse>() {
                @Override
                public TerminateJobFlowsResponse call() throws Exception {
                    return _delegateClient.terminateJobFlows(request);
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

    }

    static class InterruptedRuntimeException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public InterruptedRuntimeException(String message, InterruptedException cause) {
            super(message, cause);
        }

        public InterruptedRuntimeException(InterruptedException cause) {
            super(cause);
        }

        @Override
        public InterruptedException getCause() {
            return (InterruptedException) super.getCause();
        }

    }

    public class StepFuture {

        private final String _stepName;
        private final int _stepIndex;

        public StepFuture(String stepName, int stepIndex) {
            _stepName = stepName;
            _stepIndex = stepIndex;
        }

        public int getStepIndex() {
            return _stepIndex;
        }

        public void join() throws InterruptedException, AmazonElasticMapReduceException {
            try {
                waitUntilStepFinished(_jobFlowId, _stepName);
            } catch (InterruptedRuntimeException e) {
                throw e.getCause();
            }
        }
    }

}
