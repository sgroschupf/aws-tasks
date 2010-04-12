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

import static org.junit.Assert.*;

import org.junit.Test;

import com.amazonaws.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.elasticmapreduce.model.DescribeJobFlowsResponse;

import datameer.awstasks.aws.emr.AmazonElasticMapReduceCustomClient.JobFlowDescriptionCache;

public class AmazonElasticMapReduceCustomClientTest {

    @Test
    public void testFlowDescriptionCache() throws Exception {
        int maxCacheTime = 200;
        JobFlowDescriptionCache cache = new JobFlowDescriptionCache(maxCacheTime);
        DescribeJobFlowsRequest request1 = new DescribeJobFlowsRequest();
        request1.withJobFlowIds("jf1");
        DescribeJobFlowsRequest request2 = new DescribeJobFlowsRequest();
        request1.withJobFlowIds("jf2");

        assertNull(cache.getResponse(request1));
        assertNull(cache.getResponse(request2));

        cache.addResponse(request1, new DescribeJobFlowsResponse());
        assertNotNull(cache.getResponse(request1));
        assertNull(cache.getResponse(request2));

        Thread.sleep(maxCacheTime * 2);
        assertNull(cache.getResponse(request1));
        assertNull(cache.getResponse(request2));
    }
}
