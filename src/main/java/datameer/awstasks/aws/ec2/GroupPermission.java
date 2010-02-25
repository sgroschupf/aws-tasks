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

class GroupPermission {

    private final String _protocol;
    private final int _fromPort;
    private final int _toPort;
    private String _sourceIpOrGroup;

    public GroupPermission(String protocol, int fromPort, int toPort) {
        this(protocol, fromPort, toPort, null);
    }

    public GroupPermission(String protocol, int fromPort, int toPort, String sourceIpOrGroup) {
        _protocol = protocol;
        _fromPort = fromPort;
        _toPort = toPort;
        _sourceIpOrGroup = sourceIpOrGroup;
    }

    public String getProtocol() {
        return _protocol;
    }

    public int getFromPort() {
        return _fromPort;
    }

    public int getToPort() {
        return _toPort;
    }

    public String getSourceIpOrGroup() {
        return _sourceIpOrGroup;
    }

    public void setSourceIpOrGroup(String sourceIpOrGroup) {
        _sourceIpOrGroup = sourceIpOrGroup;
    }

    @Override
    public String toString() {
        String string = _protocol + ":" + _fromPort + "->" + _toPort;
        if (_sourceIpOrGroup != null) {
            string += " | " + _sourceIpOrGroup;
        }
        return string;
    }

    public static GroupPermission createStandardSsh() {
        return new GroupPermission("tcp", 22, 22);
    }

}
