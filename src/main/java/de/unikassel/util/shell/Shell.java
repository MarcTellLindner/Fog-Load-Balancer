package de.unikassel.util.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Allows execution of commands on shell.
 */
public class Shell {

    private final List<ShellCommand> shellCommands = new ArrayList<>();

    /**
     * Add a mew command to this shell.
     *
     * @param shellCommand The command to add.
     * @return The shell-instance this method was called on.
     */
    public Shell addShellCommand(ShellCommand shellCommand) {
        this.shellCommands.add(shellCommand);
        return this;
    }

    /**
     * Add a command to this shell, that requests sudo-rights.
     *
     * @param password The sudo-password.
     * @return The shell-instance this method was called on.
     */
    public Shell addSudoRightCommand(String password) {
        return this.addShellCommand(new ShellCommand("echo").withArgs(password, "|", "sudo", "-S", "true"));
    }

    /**
     * Execute all commands of this shell.
     *
     * @return The result of the execution.
     * @throws IOException If a problems occurs executing the commands.
     */
    public ShellResult execute() throws IOException {
        Process process = new ProcessBuilder().command("/bin/sh", "-c",
                shellCommands.stream().map(Objects::toString)
                        .collect(Collectors.joining(" && ")))
                .start();


        int exitVal;
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
