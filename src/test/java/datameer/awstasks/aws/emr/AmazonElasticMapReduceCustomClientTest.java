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
