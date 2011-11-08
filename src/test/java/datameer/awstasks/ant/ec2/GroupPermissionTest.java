package datameer.awstasks.ant.ec2;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;

import datameer.awstasks.aws.ec2.GroupPermission;

public class GroupPermissionTest {

    @Test
    public void testMatches_ips() {
        GroupPermission groupPermission = new GroupPermission("tcp", 1433, 1433, "0.0.0.0/0");
        IpPermission ipPermission = new IpPermission();
        ipPermission.setIpProtocol("tcp");
        ipPermission.setToPort(65535);
        ipPermission.setFromPort(0);
        Collection<String> ips = new ArrayList<String>();
        ips.add("127.0.0.1");
        ipPermission.setIpRanges(ips);
        assertFalse(groupPermission.matches(ipPermission));

        ips.clear();
        ips.add("0.0.0.0/0");
        ipPermission.setIpRanges(ips);
        assertTrue(groupPermission.matches(ipPermission));
    }

    @Test
    public void testMatches_groupWithSamePort() {
        GroupPermission groupPermission = new GroupPermission("tcp", 1433, 1433, "0.0.0.0/0");
        IpPermission ipPermission = new IpPermission();
        ipPermission.setIpProtocol("tcp");
        ipPermission.setToPort(65535);
        ipPermission.setFromPort(0);
        Collection<UserIdGroupPair> ips = new ArrayList<UserIdGroupPair>();
        ips.add(new UserIdGroupPair().withGroupId("0.0.0.0/0").withGroupName("0.0.0.0/0").withUserId("0.0.0.0/0"));
        ipPermission.setUserIdGroupPairs(ips);
        assertFalse(groupPermission.matches(ipPermission));
    }
}
