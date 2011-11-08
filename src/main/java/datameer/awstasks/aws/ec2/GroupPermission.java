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

import com.amazonaws.services.ec2.model.IpPermission;

public class GroupPermission {

    private String _protocol;
    private int _fromPort;
    private int _toPort = -1;
    private String _sourceIp;

    public GroupPermission() {
        // default constructor for ant
    }

    public GroupPermission(String protocol, int fromPort, int toPort, String sourceIpOrGroup) {
        _protocol = protocol;
        _fromPort = fromPort;
        _toPort = toPort;
        _sourceIp = sourceIpOrGroup;
    }

    public void setProtocol(String protocol) {
        _protocol = protocol;
    }

    public String getProtocol() {
        return _protocol;
    }

    public void setFromPort(int fromPort) {
        _fromPort = fromPort;
    }

    public int getFromPort() {
        return _fromPort;
    }

    public void setToPort(int toPort) {
        _toPort = toPort;
    }

    public int getToPort() {
        return _toPort;
    }

    public String getSourceIp() {
        return _sourceIp;
    }

    public void setSourceIp(String sourceIpOrGroup) {
        _sourceIp = sourceIpOrGroup;
    }

    public IpPermission toIpPermission() {
        return new IpPermission().withIpProtocol(getProtocol()).withFromPort(getFromPort()).withToPort(getToPort()).withIpRanges(getSourceIp());
    }

    @Override
    public String toString() {
        String string = _protocol + ":" + _fromPort + "->" + _toPort;
        if (_sourceIp != null) {
            string += " | " + _sourceIp;
        }
        return string;
    }

    public static GroupPermission createStandardSsh() {
        return new GroupPermission("tcp", 22, 22, "0.0.0.0/0");
    }

    public boolean matches(IpPermission ipPermission) {
        boolean sourceAllowed = false;
        if (!ipPermission.getIpRanges().isEmpty()) {
            sourceAllowed = ipPermission.getIpRanges().contains(_sourceIp);
        } else if (!ipPermission.getUserIdGroupPairs().isEmpty()) {
            sourceAllowed = false;
        }

        return ipPermission.getFromPort() <= getFromPort() && ipPermission.getToPort() >= getToPort() && getProtocol().equalsIgnoreCase(ipPermission.getIpProtocol()) && sourceAllowed;

    }
}
