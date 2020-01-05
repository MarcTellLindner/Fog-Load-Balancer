package de.unikassel;

import de.unikassel.util.nativ.jna.ThreadUtil;
import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class WorkerNodeTest {

    @Test
    public void test() throws IOException {
        ShellResult execute = new Shell().addShellCommand(
                new ShellCommand(
                        String.format("java -jar %s %d '{inspectit:{config:{file-based:{path:\"%s\"}}}}'",
                                System.getenv("inspectit"),
                                ThreadUtil.getThreadId(),
                                System.getenv("config"))
                )
        ).execute();

        assertEquals(0, execute.exitVal);

        try (
                WorkerNode workerNode = new WorkerNode()
        ) {
            workerNode.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
