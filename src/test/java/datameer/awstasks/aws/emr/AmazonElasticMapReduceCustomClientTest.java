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

import static org.hamcrest.Matchers.*;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import java.util.concurrent.Callable;

import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsResult;
import com.amazonaws.services.sns.model.AuthorizationErrorException;

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

        cache.addResponse(request1, new DescribeJobFlowsResult());
        assertNotNull(cache.getResponse(request1));
        assertNull(cache.getResponse(request2));

        Thread.sleep(maxCacheTime * 2);
        assertNull(cache.getResponse(request1));
        assertNull(cache.getResponse(request2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDoWithRetry_ThrottleException() throws Exception {
        AmazonElasticMapReduceCustomClient client = new AmazonElasticMapReduceCustomClient("dummy", "dummy");
        client.setRequestInterval(100);

        Callable callable = mock(Callable.class);
        AmazonServiceException exception = new AmazonServiceException("Rate exceeded");
        exception.setErrorCode("Throttling");
        exception.setStatusCode(400);
        when(callable.call()).thenThrow(exception, exception, exception).thenReturn(new Object());

        long startTime = System.currentTimeMillis();
        Object result = client.doThrottleSafe(callable);
        assertNotNull(result);
        assertThat((System.currentTimeMillis() - startTime), greaterThanOrEqualTo(3 * client.getRequestInterval()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDoWithRetry_ReadTimeOut() throws Exception {
        AmazonElasticMapReduceCustomClient client = new AmazonElasticMapReduceCustomClient("dummy", "dummy");
        client.setRequestInterval(100);

        Callable callable = mock(Callable.class);
        AmazonClientException exception = new AmazonClientException("Unable to execute HTTP request: Read timed out");
        when(callable.call()).thenThrow(exception, exception, exception).thenReturn(new Object());

        long startTime = System.currentTimeMillis();
        Object result = client.doThrottleSafe(callable);
        assertNotNull(result);
        assertThat((System.currentTimeMillis() - startTime), greaterThanOrEqualTo(3 * client.getRequestInterval()));

        // now exceed retries
        client.setMaxRetriesOnConnectionErrors(2);
        when(callable.call()).thenThrow(exception, exception, exception).thenReturn(new Object());
        try {
            client.doThrottleSafe(callable);
            fail("should throw exception");
        } catch (Exception e) {
            assertSame(exception, e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDoWithRetry_NoRetry() throws Exception {
        AmazonElasticMapReduceCustomClient client = new AmazonElasticMapReduceCustomClient("dummy", "dummy");
        client.setRequestInterval(100);

        Callable callable = mock(Callable.class);
        AmazonClientException exception = new AuthorizationErrorException("auth error");
        when(callable.call()).thenThrow(exception).thenReturn(new Object());

        try {
            client.doThrottleSafe(callable);
            fail("should throw exception");
        } catch (Exception e) {
            assertSame(exception, e);
        }
    }
}
