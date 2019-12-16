package de.unikassel.cgroup.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Shell {

    private final List<ShellCommand> shellCommands = new ArrayList<>();

    public Shell addShellCommand(ShellCommand shellCommand) {
        this.shellCommands.add(shellCommand);
        return this;
    }

    public Shell addSudoRightCommand(String password) {
        return this.addShellCommand(new ShellCommand("echo").withArgs(password, "|", "sudo", "-S", "true"));
    }

    public ShellResult execute() throws IOException {
        Process process = new ProcessBuilder().command("/bin/sh", "-c",
                shellCommands.stream().map(Objects::toString)
                        .collect(Collectors.joining(" && ")))
                .start();


        int exitVal = 0;
        try {
            exitVal = process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Was interrupted while executing command", e);
        }
        List<String> out = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .lines().collect(Collectors.toList());
        List<String> err = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                .lines().collect(Collectors.toList());

        return new ShellResult(exitVal, out, err);
    }
}
