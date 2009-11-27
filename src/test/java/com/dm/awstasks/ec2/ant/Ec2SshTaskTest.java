package com.dm.awstasks.ec2.ant;

import static org.junit.Assert.*;

import static org.mockito.Matchers.*;

import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import com.dm.awstasks.ec2.InstanceGroup;
import com.dm.awstasks.ec2.ant.model.SshExec;
import com.dm.awstasks.ec2.ssh.SshClient;

public class Ec2SshTaskTest {

    private InstanceGroup _instanceGroup = mock(InstanceGroup.class);
    private SshClient _sshClient = mock(SshClient.class);
    private Ec2SshTask _sshTask = new Ec2SshTask(_instanceGroup);

    @Before
    public void setUp() throws Exception {
        when(_instanceGroup.createSshClient(null, null)).thenReturn(_sshClient);
        _sshTask.setGroupName("testGroup");
        _sshTask.setProject(new Project());
    }

    @Test
    public void testSshExecVariableSubstitution() throws Exception {
        String command1 = "hostname";
        String outpuProperty = "prop.hostnames";
        createSshExec(_sshTask, command1, outpuProperty);
        createSshExec(_sshTask, "echo $prop.hostnames", null);
        createSshExec(_sshTask, "echo ${prop.hostnames}", null);

        String hostName1 = "host1";
        writeToOutputStream(hostName1).when(_sshClient).executeCommand(eq(command1), (OutputStream) notNull());

        _sshTask.execute();
        verify(_sshClient).executeCommand(eq(command1), (OutputStream) notNull());
        verify(_sshClient, times(2)).executeCommand(eq("echo " + hostName1), (OutputStream) notNull());
        assertEquals(hostName1, _sshTask.getProject().getProperty(outpuProperty));
    }

    private SshExec createSshExec(Ec2SshTask sshTask, String command, String outpuProperty) {
        SshExec sshExec = new SshExec();
        sshExec.setCommand(command);
        sshExec.setOutputProperty(outpuProperty);
        sshTask.addExec(sshExec);
        return sshExec;
    }

    private Stubber writeToOutputStream(final String string) {
        return doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ByteArrayOutputStream outStream = (ByteArrayOutputStream) invocation.getArguments()[1];
                outStream.write(string.getBytes());
                return null;
            }
        });
    }
}
