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
package datameer.awstasks.aws;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;

import datameer.awstasks.AbstractTest;
import datameer.awstasks.util.Ec2Configuration;

public abstract class AbstractAwsIntegrationTest extends AbstractTest {

    protected static final Logger LOG = Logger.getLogger(AbstractAwsIntegrationTest.class);

    protected static Ec2Configuration _ec2Conf;

    @BeforeClass
    public static void readEc2Properties() throws IOException {
        _ec2Conf = new Ec2Configuration();
        if (!_ec2Conf.isEc2Configured()) {
            fail("can't run integration test without a properly configured src/it/resources/ec2.properties");
        }
    }

    @Before
    public void checkConfiguration() {
        if (!_ec2Conf.isEc2Configured()) {
            fail("can't run integration test without a properly configured src/it/resources/ec2.properties");
        } else if (!new File(_ec2Conf.getPrivateKeyFile()).exists()) {
            fail(String.format("private key file '%s' not exists", _ec2Conf.getPrivateKeyFile()));
        }
    }

}
