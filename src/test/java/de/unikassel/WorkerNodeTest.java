package de.unikassel;

import de.unikassel.util.nativ.jna.ThreadUtil;
import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;
import org.junit.Test;

import java.io.IOException;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;

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
            PermissionCollection permissions = new Permissions();
            permissions.add(new AllPermission()); // Allow everything while testing
            workerNode.start(permissions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
