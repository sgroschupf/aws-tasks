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
package datameer.awstasks.aws.ec2;

import static org.fest.assertions.Assertions.*;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import awstasks.com.amazonaws.services.ec2.model.IpPermission;
import awstasks.com.amazonaws.services.ec2.model.UserIdGroupPair;

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

    @Test
    public void testIsIpDefinition() throws Exception {
        assertThat(GroupPermission.isIpDefinition("127.0.0.1")).isTrue();
        assertThat(GroupPermission.isIpDefinition("0.0.0.0/0")).isTrue();
        assertThat(GroupPermission.isIpDefinition("security-group")).isFalse();
    }
}
