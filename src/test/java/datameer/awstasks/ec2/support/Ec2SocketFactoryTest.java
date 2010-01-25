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
package datameer.awstasks.ec2.support;

import static org.junit.Assert.*;

import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;

import datameer.awstasks.ec2.support.Ec2SocketFactory;

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
