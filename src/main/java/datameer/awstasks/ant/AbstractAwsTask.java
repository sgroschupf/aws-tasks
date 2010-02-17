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
package datameer.awstasks.ant;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Task;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import com.xerox.amazonws.ec2.Jec2;

public abstract class AbstractAwsTask extends Task {

    protected static final Logger LOG = Logger.getLogger(AbstractAwsTask.class);

    protected String _accessKey;
    protected String _accessSecret;

    public String getAccessKey() {
        return _accessKey;
    }

    public void setAccessKey(String accessKey) {
        _accessKey = accessKey;
    }

    public String getAccessSecret() {
        return _accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        _accessSecret = accessSecret;
    }

    public Jec2 createJec2() {
        return new Jec2(_accessKey, _accessSecret);
    }

    public S3Service createS3Service() throws S3ServiceException {
        AWSCredentials awsCredentials = new AWSCredentials(_accessKey, _accessSecret);
        return new RestS3Service(awsCredentials);
    }

}
