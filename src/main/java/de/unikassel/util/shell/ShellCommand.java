package de.unikassel.util.shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A command executable with the {@link Shell}.
 */
public class ShellCommand {
    private final String command;
    private final List<String> args;


    /**
     * Create a new command.
     *
     * @param command The command.
     */
    public ShellCommand(Object command) {
        this(command, false);
    }

    /**
     * Create a new command.
     *
     * @param command The new command.
     * @param sudo    Sudo-rights required.
     */
    public ShellCommand(Object command, boolean sudo) {
        this.command = (sudo ? "sudo " : "") + command;
        this.args = new ArrayList<>();
    }

    /**
     * Varargs-version of {@link ShellCommand#withArgs(List)}.
     */
    public ShellCommand withArgs(Object... args) {
        return this.withArgs(Arrays.asList(args));
    }

    /**
     * Add any amount of arguments.
     *
     * @param args Arguments to add.
     * @return The shell command this method was called on.
     */
    public ShellCommand withArgs(List<Object> args) {
        this.args.addAll(args.stream().map(Objects::toString).collect(Collectors.toList()));
        return this;
    }

    @Override
    public String toString() {
        return String.join(" ", command, String.join(" ", args));
    }
}
