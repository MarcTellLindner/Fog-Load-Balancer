package de.unikassel.util.shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ShellCommand {
    private final String command;
    private final List<String> args;


    public ShellCommand(Object command) {
        this(command, false);
    }

    public ShellCommand(Object command, boolean sudo) {
        this.command = (sudo ? "sudo " : "") + command;
        this.args = new ArrayList<>();
    }

    public ShellCommand withArgs(Object... args) {
        return this.withArgs(Arrays.asList(args));
    }

    public ShellCommand withArgs(List<Object> args) {
        this.args.addAll(args.stream().map(Objects::toString).collect(Collectors.toList()));
        return this;
    }

    @Override
    public String toString() {
        return String.join(" ", command, String.join(" ", args));
    }
}
