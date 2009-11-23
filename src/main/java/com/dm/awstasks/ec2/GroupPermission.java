package com.dm.awstasks.ec2;

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
