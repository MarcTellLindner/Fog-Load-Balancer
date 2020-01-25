package de.unikassel.cgroup;

import de.unikassel.cgroup.options.Option;
import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;
import de.unikassel.util.nativ.jna.ThreadUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for creating a cgroup.
 */
public class CGroup {
    public final String name;
    private final Map<Controller, Map<Option, String>> values;

    public CGroup(String name, Controller... controllers) {
        this(name, Arrays.asList(controllers));
    }

    /**
     * Create a new cgroup. that supports the specified controllers
     *
     * @param name        Name of this cgroup
     * @param controllers supported controllers
     */
    public CGroup(String name, Collection<Controller> controllers) {
        this.name = name;
        this.values = new HashMap<>();
        controllers.forEach(c -> values.put(c, new HashMap<>()));

    }

    /**
     * Add a new {@link Option} to this cgroup or override an existing one.
     *
     * @param option The option.
     * @param value  The value of the option.
     * @return The {@link CGroup} this method was called on.
     */
    public CGroup withOption(Option option, Object value) {
        this.values.get(option.getController()).put(option, "" + value);
        return this;
    }


    /**
     * Create the cgroup with all {@link Controller}s used.
     *
     * @param sudoPW The sudo-password to allow the creation of the cgroup.
     * @return The result of the call for creating the cgroup.
     * @throws IOException In case of exceptions when executing cgroup create.
     */
    public ShellResult create(String sudoPW) throws IOException {
        return this.create(sudoPW, values.keySet());
    }

    /**
     * Varargs-version of {@link CGroup#create(String, Collection)}.
     */
    public ShellResult create(String sudoPW, Controller... controllers) throws IOException {
        return this.create(sudoPW, Arrays.asList(controllers));
    }

    /**
     * Create the cgroup.
     *
     * @param sudoPW      The sudo-password to allow the creation of the cgroup.
     * @param controllers The controllers to use for the cgroup.
     * @return The result of the call to create the cgroup.
     * @throws IOException In case of exceptions when executing cgroup create.
     */
    public ShellResult create(String sudoPW, Collection<Controller> controllers) throws IOException {
        Shell shell = new Shell().addSudoRightCommand(sudoPW)
                .addShellCommand(
                        new ShellCommand(Command.CGCREATE, true)
                                .withArgs("-g", qualifyName(controllers, this.name))
                );
        for (Controller controller : controllers) {
            for (Map.Entry<Option, String> entry : values.get(controller).entrySet()) {
                shell.addShellCommand(
                        new ShellCommand(Command.CGSET, true)
                                .withArgs(
                                        "-r",
                                        String.format("%s.%s=%s", controller, entry.getKey(), entry.getValue()),
                                        name
                                )
                );
            }
        }
        return shell.execute();
    }

    /**
     * Classify the current thread to this cgroup using all of its controllers.
     *
     * @param sudoPW The sudo-password to allow usage of cgroup classify.
     * @return The result of the call to classify the thread.
     * @throws IOException In case of exceptions when executing cgroup classify.
     */
    public ShellResult classify(String sudoPW) throws IOException {
        return this.classify(sudoPW, values.keySet());
    }

    /**
     * Varargs-version of {@link CGroup#classify(String, Collection)}.
     */
    public ShellResult classify(String sudoPW, Controller... controllers) throws IOException {
        return this.classify(sudoPW, Arrays.asList(controllers));
    }


    /**
     * Classify the current thread to this cgroup.
     *
     * @param sudoPW The sudo-password to allow usage of cgroup classify.
     * @param controllers The controllers of the cgroup to use.
     * @return The result of the call to classify the thread.
     * @throws IOException In case of exceptions when executing cgroup classify.
     */
    public ShellResult classify(String sudoPW, Collection<Controller> controllers) throws IOException {
        Shell shell = new Shell().addSudoRightCommand(sudoPW)
                .addShellCommand(
                        new ShellCommand(Command.CGCLASSIFY, true)
                                .withArgs("-g", qualifyName(controllers, this.name), ThreadUtil.getThreadId())
                );
        return shell.execute();
    }

    /**
     * Delete the cgroup with all of its controllers.
     *
     * @param sudoPW The sudo-password to allow the deletion of the cgroup.
     * @return The result of the call to delete the cgroup.
     * @throws IOException In case of exceptions when executing cgroup delete.
     */
    public ShellResult delete(String sudoPW) throws IOException {
        return this.delete(sudoPW, this.values.keySet());
    }

    /**
     * Varargs-version of {@link CGroup#delete(String, Collection)}.
     */
    public ShellResult delete(String sudoPW, Controller... controllers) throws IOException {
        return this.delete(sudoPW, Arrays.asList(controllers));
    }

    /**
     * Delete the cgroup with all of its controllers.
     *
     * @param sudoPW The sudo-password to allow the deletion of the cgroup.
     * @param controllers The controllers of the cgroup to delete.
     * @return The result of the call to delete the cgroup.
     * @throws IOException In case of exceptions when executing cgroup delete.
     */
    public ShellResult delete(String sudoPW, Collection<Controller> controllers) throws IOException {
        Shell shell = new Shell().addSudoRightCommand(sudoPW)
                .addShellCommand(
                        new ShellCommand(Command.CGDELETE, true)
                                .withArgs("-g", qualifyName(controllers, this.name))
                );
        return shell.execute();
    }

    private String qualifyName(Collection<Controller> controllers, String name) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Controller> iter = controllers.iterator(); iter.hasNext();) {
            Controller ctrl = iter.next();
            sb.append(ctrl);
            if (iter.hasNext()) {
                sb.append(",");
            }
        }
        return String.format("%s:%s", sb, name);
    }
}
