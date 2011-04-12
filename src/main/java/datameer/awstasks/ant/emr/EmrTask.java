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
package datameer.awstasks.ant.emr;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import datameer.awstasks.ant.AbstractAwsTask;
import datameer.awstasks.ant.emr.model.EmrCommand;
import datameer.awstasks.ant.emr.model.EmrStartCommand;
import datameer.awstasks.ant.emr.model.EmrStopCommand;
import datameer.awstasks.ant.s3.model.CreateBucketCommand;
import datameer.awstasks.aws.emr.EmrCluster;
import datameer.awstasks.aws.emr.EmrSettings;

public class EmrTask extends AbstractAwsTask {

    private String _clusterName;
    private String _s3Bucket;
    private List<EmrCommand> _emrCommands = new ArrayList<EmrCommand>();
    private boolean _normalizeBucketName;

    public EmrTask() {
        // default constructor - needed by ant
    }

    public String getClusterName() {
        return _clusterName;
    }

    public void setClusterName(String name) {
        _clusterName = name;
    }

    public void setS3Bucket(String bucket) {
        _s3Bucket = bucket;
    }

    public String getS3Bucket() {
        return _s3Bucket;
    }

    public void setNormalizeBucketName(boolean normalizeBucketName) {
        _normalizeBucketName = normalizeBucketName;
    }

    public boolean isNormalizeBucketName() {
        return _normalizeBucketName;
    }

    public EmrCluster createEmrCluster() {
        String s3Bucket = _s3Bucket;
        if (isNormalizeBucketName()) {
            s3Bucket = CreateBucketCommand.normalizeBucketName(s3Bucket);
        }
        EmrSettings settings = new EmrSettings(_clusterName, _accessKey, s3Bucket);
        return new EmrCluster(settings, _accessSecret);
    }

    @Override
    public void execute() throws BuildException {
        System.out.println("executing " + getClass().getSimpleName());
        try {
            EmrCluster emrService = createEmrCluster();
            for (EmrCommand emrCommand : _emrCommands) {
                System.out.println("executing " + emrCommand);
                emrCommand.execute(emrService);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public void addStartCluster(EmrStartCommand command) {
        _emrCommands.add(command);
    }

    public void addStopCluster(EmrStopCommand command) {
        _emrCommands.add(command);
    }

}
