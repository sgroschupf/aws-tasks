package com.dm.awstasks.ec2.support;

import static org.junit.Assert.*;

import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;

public class Ec2SocketFactoryTest {

    @Test
    public void testAddMapping() throws Exception {
        String privateIp = "10-244-155-31";
        String privateDns = "ip-10-244-155-31.ec2.internal";
        String publicDns = "ec2-67-202-21-75.compute-1.amazonaws.com";

        Ec2SocketFactory.addHostMapping(privateIp, publicDns);
        Ec2SocketFactory.addHostMapping(privateDns, publicDns);
        assertTrue(Ec2SocketFactory.containsHostMapping(privateIp));
        assertTrue(Ec2SocketFactory.containsHostMapping(privateDns));
    }

    @Test
    public void testAddressTranslation() throws Exception {
        Ec2SocketFactory.addHostMapping("private", "localhost");

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(null);

        Socket socket1 = new Ec2SocketFactory().createSocket("localhost", serverSocket.getLocalPort());
        Socket socket2 = new Ec2SocketFactory().createSocket("private", serverSocket.getLocalPort());
        socket1.close();
        socket2.close();
        serverSocket.close();
    }
}
