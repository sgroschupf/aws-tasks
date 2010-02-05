package datameer.awstasks.ssh;

import static org.mockito.Mockito.*;

import org.junit.Test;

import datameer.awstasks.exec.ExecOutputHandler;
import datameer.awstasks.ssh.SshExecDelegateCommand.ToLineOutputStream;

public class SshExecDelegateCommandTest {

    @Test
    public void testOutputStream_NoEof() throws Exception {
        ExecOutputHandler handler = mock(ExecOutputHandler.class);
        ToLineOutputStream stream = new ToLineOutputStream(handler);

        stream.write("hello handler".getBytes());
        verify(handler, times(1)).handleLine("hello handler");
        verifyNoMoreInteractions(handler);

        stream.write("hello handler\n".getBytes());
        verify(handler, times(2)).handleLine("hello handler");
        verifyNoMoreInteractions(handler);

        stream.write("\nhello handler".getBytes());
        verify(handler, times(1)).handleLine("");
        verify(handler, times(3)).handleLine("hello handler");
        verifyNoMoreInteractions(handler);

        stream.write("1\n2".getBytes());
        verify(handler, times(1)).handleLine("1");
        verify(handler, times(1)).handleLine("2");
        verifyNoMoreInteractions(handler);

        stream.write("a\nb\n".getBytes());
        verify(handler, times(1)).handleLine("a");
        verify(handler, times(1)).handleLine("b");
        verifyNoMoreInteractions(handler);
    }
}
