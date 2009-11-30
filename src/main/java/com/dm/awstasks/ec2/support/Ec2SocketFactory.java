package com.dm.awstasks.ec2.support;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.SocketFactory;

import org.apache.log4j.Logger;

/**
 * A socket factory which translate private ec2 addresses (hostnames/ips) into public one.
 */
public class Ec2SocketFactory extends SocketFactory {

    protected static final Logger LOG = Logger.getLogger(Ec2SocketFactory.class);

    protected static Map<String, String> _publicAddressesByPrivateAddresses = new HashMap<String, String>();

    /**
     * Parameters can be ip or dns-hostname.
     * 
     * @param privateAddress
     * @param publicAddress
     */
    public static void addHostMapping(String privateAddress, String publicAddress) {
        _publicAddressesByPrivateAddresses.put(privateAddress, publicAddress);
    }

    public static void removeHostMapping(String privateAddress) {
        _publicAddressesByPrivateAddresses.remove(privateAddress);
    }

    public static boolean containsHostMapping(String privateAddress) {
        return _publicAddressesByPrivateAddresses.containsKey(privateAddress);
    }

    public static void clearHostMappings() {
        _publicAddressesByPrivateAddresses.clear();
    }

    @Override
    public Socket createSocket() throws IOException {
        return new Ec2Socket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port));
        return socket;

    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localHost, localPort));
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = createSocket();
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(address, port));
        return socket;
    }

    protected class Ec2Socket extends Socket {

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            super.connect(translateSocketAddres((InetSocketAddress) endpoint));
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            super.connect(translateSocketAddres((InetSocketAddress) endpoint), timeout);
        }

        private SocketAddress translateSocketAddres(InetSocketAddress endpoint) {
            String puplicHostname = _publicAddressesByPrivateAddresses.get(endpoint.getHostName());
            if (puplicHostname != null) {
                LOG.debug("translate private address '" + endpoint.getHostName() + "' into public address '" + puplicHostname + "'");
                endpoint = new InetSocketAddress(puplicHostname, endpoint.getPort());
            }
            return endpoint;
        }
    }
}
