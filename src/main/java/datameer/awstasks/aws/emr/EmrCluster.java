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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.model.AddJobFlowStepsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsResult;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowDetail;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.PlacementType;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;
import com.amazonaws.services.elasticmapreduce.model.StepDetail;
import com.amazonaws.services.elasticmapreduce.model.TerminateJobFlowsRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.SelectRequest;
import datameer.com.google.common.base.Preconditions;
import datameer.com.google.common.base.Predicate;
import datameer.com.google.common.collect.Collections2;

import datameer.awstasks.aws.concurrent.ObjectLock;
import datameer.awstasks.aws.emr.JobFlowState.StateCategory;
import datameer.awstasks.util.S3Util;

/**
 * Allows access and management of amazons elastic map-reduce. One emr cluster maps to one job flow.
 */
public class EmrCluster {

    private static final StepConfig DEBUG_STEP = createDebugStep();

    private static StepConfig createDebugStep() {
        StepConfig debugStep = new StepConfig();
        debugStep.setName("Setup Hadoop Debugging");
        debugStep.setActionOnFailure("TERMINATE_JOB_FLOW");

        HadoopJarStepConfig hadoopJarStepConfig = new HadoopJarStepConfig();
        hadoopJarStepConfig.setJar("s3://us-east-1.elasticmapreduce/libs/script-runner/script-runner.jar");
        hadoopJarStepConfig.getArgs().add("s3://us-east-1.elasticmapreduce/libs/state-pusher/0.1/fetch");
        debugStep.setHadoopJarStep(hadoopJarStepConfig);
        return debugStep;
    }

    protected static final Logger LOG = Logger.getLogger(EmrCluster.class);

    private final EmrSettings _settings;
    private final String _accessSecret;
    protected AmazonElasticMapReduceCustomClient _emrWebService;
    private AmazonS3 _s3Service;
    protected AmazonSimpleDB _simpleDB;
    protected long _startTime;
    protected volatile String _masterHost;
    protected volatile int _instanceCount;

    protected String _jobFlowId;
    protected ClusterState _clusterState = ClusterState.UNCONNECTED;
    private ObjectLock<String> _uploadLock = ObjectLock.create();

    // TODO jz: rethrow interrupted exceptions

    public EmrCluster(EmrSettings settings, String accessSecret) {
        _accessSecret = accessSecret;
        _settings = settings;
        _emrWebService = new AmazonElasticMapReduceCustomClient(settings.getAccessKey(), _accessSecret);
        // FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (settings.isDebugEnabled()) {
            _simpleDB = new AmazonSimpleDBClient(new BasicAWSCredentials(settings.getAccessKey(), accessSecret));
        }
    }

    public String getName() {
        return getSettings().getClusterName();
    }

    public EmrSettings getSettings() {
        return _settings;
    }

    public AmazonElasticMapReduce getEmrService() {
        return _emrWebService;
    }

    public void setRequestInterval(long requestInterval) {
        _emrWebService.setRequestInterval(requestInterval);
    }

    public long getRequestInterval() {
        return _emrWebService.getRequestInterval();
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

    public synchronized void startup() throws InterruptedException {
        checkConnection(false);
        _clusterState = ClusterState.STARTING;
        boolean successful = false;
        try {
            EmrSettings settings = getSettings();
            if (settings.getPrivateKeyName() == null) {
                throw new NullPointerException("privateKeyName must not be null please configure settings properly");
            }
            LOG.info("Starting job flow '" + getName() + "' ...");
            if (getRunningJobFlowDetails(false) != null) {
                throw new IllegalStateException("Job flow with name '" + getName() + "' already running.");
            }
            boolean keepAlive = true;
            JobFlowInstancesConfig jobConfig = new JobFlowInstancesConfig();
            jobConfig.setHadoopVersion(_settings.getHadoopVersion());
            jobConfig.setMasterInstanceType(settings.getMasterInstanceType().toString());
            jobConfig.setSlaveInstanceType(settings.getNodeInstanceType().toString());
            jobConfig.setInstanceCount(settings.getInstanceCount());
            jobConfig.setEc2KeyName(settings.getPrivateKeyName());
            jobConfig.setPlacement(new PlacementType());
            jobConfig.setKeepJobFlowAliveWhenNoSteps(keepAlive);

            final RunJobFlowRequest startRequest = new RunJobFlowRequest();

            startRequest.setLogUri("s3n://" + settings.getS3Bucket() + settings.getS3LogPath());
            startRequest.setInstances(jobConfig);
            startRequest.setName(getName());
            startRequest.setAdditionalInfo(_settings.getAdditionalStartInfo());
            startRequest.setBootstrapActions(_settings.getBootstrapActions());
            if (settings.isDebugEnabled()) {
                startRequest.withSteps(DEBUG_STEP);
            }
            RunJobFlowResult startResponse = _emrWebService.runJobFlow(startRequest);
            _jobFlowId = startResponse.getJobFlowId();
            waitUntilClusterStateChange(_jobFlowId, StateCategory.OPERATIONAL);
            LOG.info("elastic cluster '" + getName() + "/" + _jobFlowId + "' started, master-host is " + _masterHost);
            successful = true;
        } finally {
            if (successful) {
                _clusterState = ClusterState.CONNECTED;
            } else {
                _clusterState = ClusterState.UNCONNECTED;
                _jobFlowId = null;
            }
        }
    }

    /**
     * Disconnect this class instance from the cluster without shutting it down.
     */
    public void disconnect() {
        _jobFlowId = null;
        _startTime = 0;
        _clusterState = ClusterState.UNCONNECTED;
        // shutdownS3Service();
    }

    public synchronized void shutdown() throws InterruptedException {
        checkConnection(true);
        _clusterState = ClusterState.STOPPING;
        _emrWebService.terminateJobFlows(new TerminateJobFlowsRequest().withJobFlowIds(_jobFlowId));
        waitUntilClusterStateChange(_jobFlowId, StateCategory.DEAD);
        disconnect();
    }

    /**
     * Connect by cluster name.
     * 
     * @throws InterruptedException
     */
    public void connectByName() throws InterruptedException {
        checkConnection(false);
        JobFlowDetail jobFlow = getRunningJobFlowDetails(true);
        connectById(jobFlow.getJobFlowId());
    }

    /**
     * Connect to a cluster/jobFlow with the given id.
     * 
     * @param jobFlowId
     * @throws InterruptedException
     */
    public void connectById(String jobFlowId) throws InterruptedException {
        checkConnection(false);
        _jobFlowId = jobFlowId;
        waitUntilClusterStateChange(_jobFlowId, StateCategory.OPERATIONAL);
        LOG.info("connected to elastic cluster '" + getName() + "/" + _jobFlowId + "', master-host is " + _masterHost);
        _clusterState = ClusterState.CONNECTED;
    }

    /**
     * Connects to EMR cluster and equilibrate the local state with the remote state.
     * 
     * @throws InterruptedException
     */
    public void synchronizeState() throws InterruptedException {
        if (_clusterState == ClusterState.UNCONNECTED) {
            try {
                connectByName();
                return;// we have a new state
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                return; // there is no cluster up
            }
        }

        JobFlowDetail jobFlowDetail = getJobFlowDetail(_jobFlowId);
        JobFlowState state = JobFlowState.valueOf(jobFlowDetail.getExecutionStatusDetail().getState());
        if (!state.isOperational() && _clusterState == ClusterState.CONNECTED) {
            disconnect();
        }
    }

    public ClusterState getState() {
        return _clusterState;
    }

    public boolean isIdle() {
        if (_clusterState != ClusterState.CONNECTED) {
            return false;
        }
        JobFlowState state = JobFlowState.valueOf(getJobFlowDetail(_jobFlowId).getExecutionStatusDetail().getState());
        return state.isIdle();
    }

    public String getJobFlowId() {
        return _jobFlowId;
    }

    protected void checkConnection(boolean shouldRun) {
        if (shouldRun && (_clusterState == ClusterState.UNCONNECTED || _clusterState == ClusterState.STOPPING)) {
            throw new IllegalStateException("not connected to cluster/jobFlow");
        }
        if (!shouldRun && _clusterState == ClusterState.CONNECTED) {
            throw new IllegalStateException("already connected to cluster/jobFlow");
        }
    }

    public StepFuture executeJobStep(String name, File jobJar, String... args) {
        return executeJobStep(name, jobJar, null, args);
    }

    public StepFuture executeJobStep(String name, File jobJar, Class<?> mainClass, String... args) {
        return executeJobStep(name, jobJar, jobJar.getName(), mainClass, args);
    }

    public StepFuture executeJobStep(String name, File jobJar, String s3JobJarName, Class<?> mainClass, String... args) {
        checkConnection(true);
        HadoopJarStepConfig jarConfig = new HadoopJarStepConfig();
        if (jobJar != null) {
            String s3JobJarUri = uploadingJobJar(jobJar, s3JobJarName);
            jarConfig.setJar(s3JobJarUri);
        }
        if (mainClass != null) {
            jarConfig.setMainClass(mainClass.getName());
        }
        jarConfig.setArgs(Arrays.asList(args));
        StepConfig stepConfig = new StepConfig();
        stepConfig.setName(name);
        stepConfig.setActionOnFailure("CONTINUE");
        stepConfig.setHadoopJarStep(jarConfig);
        _emrWebService.addJobFlowSteps(new AddJobFlowStepsRequest().withJobFlowId(_jobFlowId).withSteps(stepConfig));
        _emrWebService.clearDescribeJobFlowCache();
        return new StepFuture(stepConfig.getName(), getStepIndex(getJobFlowDetail(_jobFlowId), name));
    }

    private String uploadingJobJar(File jobJar, String s3JobJarName) {
        if (_s3Service == null) {
            _s3Service = new AmazonS3Client(new BasicAWSCredentials(getSettings().getAccessKey(), _accessSecret));
        }

        _uploadLock.lock(jobJar.getAbsolutePath());
        try {
            String s3JobJarPath = new File(getSettings().getS3JobJarBasePath(), s3JobJarName).getPath();
            String s3Bucket = getSettings().getS3Bucket();
            if (!_s3Service.doesBucketExist(s3Bucket)) {
                throw new IllegalStateException("s3 bucket '" + s3Bucket + "' does not exists");
            }
            if (!S3Util.existsFile(_s3Service, s3Bucket, s3JobJarPath)) {
                LOG.info("uploading " + jobJar + " to " + s3JobJarPath);
                S3Util.uploadFile(_s3Service, s3Bucket, jobJar, s3JobJarPath);
            } else {
                LOG.info("using cached job-jar: " + s3JobJarPath);
            }
            return "s3n://" + getSettings().getAccessKey() + "@" + s3Bucket + s3JobJarPath;
        } finally {
            _uploadLock.unlock(jobJar.getAbsolutePath());
        }
    }

    private void waitUntilClusterStateChange(final String jobFlowId, final StateCategory targetState) throws InterruptedException {
        doWhileNot(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobFlowDetail jobFlowDetail = getJobFlowDetail(jobFlowId);
                String lastStateChangeReason = jobFlowDetail.getExecutionStatusDetail().getLastStateChangeReason();
                JobFlowState state = JobFlowState.valueOf(jobFlowDetail.getExecutionStatusDetail().getState());
                LOG.info("elastic cluster '" + jobFlowDetail.getName() + "/" + jobFlowId + "' in state '" + state + "'");
                boolean finished = !state.isChangingState();
                if (finished) {
                    Preconditions.checkState(state.isIn(targetState), "State transition to %s of job flow '%s' failed with state '%s' and reason '%s'", targetState, jobFlowId, state,
                            lastStateChangeReason);
                    if (state.isOperational()) {
                        _masterHost = jobFlowDetail.getInstances().getMasterPublicDnsName();
                        _instanceCount = jobFlowDetail.getInstances().getInstanceCount();
                        _startTime = jobFlowDetail.getExecutionStatusDetail().getStartDateTime().getTime();
                    } else {
                        _masterHost = null;
                        _instanceCount = 0;
                        _startTime = 0;
                    }
                }
                return finished;
            }
        }, getRequestInterval());
    }

    protected void waitUntilStepFinished(final String jobFlowId, final String stepName, final int stepIndex) throws InterruptedException {
        doWhileNot(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                StepState stepState = getStepState(jobFlowId, stepName);
                LOG.info("job step " + stepIndex + "/" + stepName + " in state '" + stepState + "'");
                boolean finished = stepState.isFinished();
                if (finished) {
                    if (!stepState.isSuccessful()) {
                        int stepIndex = getStepIndex(getJobFlowDetail(jobFlowId), stepName);
                        throw new RuntimeException("job step '" + stepName + "' (" + jobFlowId + "/" + stepIndex + ") failed with state '" + stepState + "'");
                    }
                }
                return finished;
            }

        }, getRequestInterval());
    }

    protected StepState getStepState(final String jobFlowId, final String stepName) {
        JobFlowDetail flowDetail = getJobFlowDetail(jobFlowId);
        StepDetail stepDetail = getStepDetail(flowDetail, stepName);
        StepState stepState = StepState.valueOf(stepDetail.getExecutionStatusDetail().getState());
        return stepState;
    }

    protected static void doWhileNot(Callable<Boolean> callable, long requestInterval) throws InterruptedException {
        boolean finished = false;
        do {
            Thread.sleep(requestInterval);
            try {
                finished = callable.call();
            } catch (InterruptedException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } while (!finished);
    }

    protected JobFlowDetail getJobFlowDetail(String jobFlowId) {
        DescribeJobFlowsResult describeJobFlows = _emrWebService.describeJobFlows(new DescribeJobFlowsRequest().withJobFlowIds(jobFlowId));
        List<JobFlowDetail> jobFlows = describeJobFlows.getJobFlows();
        Preconditions.checkArgument(jobFlows.size() > 0, "No job flow with id '%s' found", jobFlowId);
        Preconditions.checkState(jobFlows.size() < 2, "More then one job flow with id '%s' found", jobFlowId);
        return jobFlows.get(0);
    }

    public int getCurrentStepCount(String jobFlowId) {
        if (_clusterState != ClusterState.CONNECTED) {
            return 0;
        }
        JobFlowDetail jobFlowDetail = getJobFlowDetail(jobFlowId);
        return jobFlowDetail.getSteps().size();
    }

    protected JobFlowDetail getRunningJobFlowDetails(boolean hasToExist) {
        DescribeJobFlowsResult describeJobFlows = _emrWebService.describeJobFlows(new DescribeJobFlowsRequest().withJobFlowStates(JobFlowState.STARTING.name(), JobFlowState.BOOTSTRAPPING.name(),
                JobFlowState.WAITING.name(), JobFlowState.RUNNING.name()));
        final String jobFlowName = getName();
        Collection<JobFlowDetail> matchingJobFlows = Collections2.filter(describeJobFlows.getJobFlows(), new Predicate<JobFlowDetail>() {

            @Override
            public boolean apply(JobFlowDetail input) {
                return input.getName().equals(jobFlowName);
            }

        });
        if (matchingJobFlows.isEmpty() && !hasToExist) {
            return null;
        }
        Preconditions.checkState(matchingJobFlows.size() <= 1, "More than one job flow with name '%s' running.", jobFlowName);
        Preconditions.checkState(matchingJobFlows.size() > 0, "No job flow with name '%s' running.", jobFlowName);
        return matchingJobFlows.iterator().next();
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
        private String _domain;

        public StepFuture(String stepName, int stepIndex) {
            _stepName = stepName;
            _stepIndex = stepIndex;
        }

        public int getStepIndex() {
            return _stepIndex;
        }

        public String getStepName() {
            return _stepName;
        }

        public StepState getStepState() {
            return EmrCluster.this.getStepState(_jobFlowId, _stepName);
        }

        public StepMetadata getStepMetaData() {
            if (_simpleDB == null) {
                throw new IllegalStateException("can retrieve step metadata only when hadoop debugging enabled");
            }
            if (_domain == null) {
                _domain = getDomain();
            }
            String query = "SELECT * FROM `" + _domain + "` WHERE " + StepMetadata.JOB_FLOW_ID + " = '" + _jobFlowId + "' AND " + StepMetadata.STEP_ID + " = '" + _stepIndex + "' AND "
                    + StepMetadata.TYPE + " = 'job'";
            List<Item> items = _simpleDB.select(new SelectRequest(query)).getItems();
            if (items.size() > 1) {
                throw new IllegalStateException("found more then one (" + items.size() + ") item for query '" + query + "'");
            }
            StepMetadata stepMetadata = new StepMetadata();
            if (items.isEmpty()) {
                LOG.debug("found no items for query '" + query + "' yet...");
                return stepMetadata;
                // throw new IllegalStateException("found no items for query '" + query + "'");
            }

            for (Attribute attr : items.get(0).getAttributes()) {
                stepMetadata.add(attr.getName(), attr.getValue());
            }

            return stepMetadata;
        }

        private String getDomain() {
            List<String> domains = _simpleDB.listDomains().getDomainNames();
            for (Iterator<String> iterator = domains.iterator(); iterator.hasNext();) {
                String domain = iterator.next();
                if (!domain.startsWith("ElasticMapReduce-")) {
                    iterator.remove();
                }
            }
            Collections.sort(domains);
            Collections.reverse(domains);
            if (domains.isEmpty()) {
                throw new IllegalStateException("found no hadoop-debugging domains");
            }
            return domains.get(0);
        }

        public void join() throws InterruptedException {
            try {
                waitUntilStepFinished(_jobFlowId, _stepName, _stepIndex);
            } catch (InterruptedRuntimeException e) {
                throw e.getCause();
            }
        }
    }

    public class StepMetadata {

        public final static String JOB_ID = "jobId";
        public final static String JOB_FLOW_ID = "jobFlowId";
        public final static String JOB_INDEX = "jobIndex";
        public final static String JOB_STATE = "jobState";
        public final static String TYPE = "type";
        public final static String STEP_ID = "stepId";
        public final static String USERNAME = "username";
        public final static String START_TIME = "startTime";
        public final static String NUM_TASKS = "numTasks";
        public final static String NUM_PENDING_TASKS = "numPendingTasks";
        public final static String NUM_FAILED_TASKS = "numFailedTasks";
        public final static String NUM_RUNNING_TASKS = "numRunningTasks";
        public final static String NUM_CANCELLED_TASKS = "numCancelledTasks";
        public final static String NUM_COMPLETED_TASKS = "numCompletedTasks";

        private Map<String, String> _mdMap = new HashMap<String, String>();

        public void add(String key, String value) {
            _mdMap.put(key, value);
        }

        public String get(String key) {
            return _mdMap.get(key);
        }

        public Long getAsLong(String key) {
            String value = get(key);
            if (value == null) {
                return null;
            }
            return Long.parseLong(value);
        }

        @Override
        public String toString() {
            return _mdMap.toString();
        }
    }

    public static enum ClusterState {
        CONNECTED, UNCONNECTED, STARTING, STOPPING;
    }
}
