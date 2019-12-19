package de.unikassel.cgroup;

import de.unikassel.cgroup.options.Option;
import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;
import de.unikassel.nativ.jna.ThreadUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CGroup {
    public final String name;
    private final Map<Controller, Map<Option, String>> values;

    public CGroup(String name, Controller... controllers) {
        this(name, Arrays.asList(controllers));
    }

    /**Create a new cgroup. that supports the specified controllers
     *
     * @param name Name of this cgroup
     * @param controllers supported controllers
     */
    public CGroup(String name, Collection<Controller> controllers) {
        this.name = name;
        Map<Controller, Map<Option, String>> values = new HashMap<>();
        controllers.forEach(c -> values.put(c, new HashMap<>()));
        this.values = Collections.unmodifiableMap(values);

    }

    /**
     * @param option 
     * @param value
     * @return
     */
    public CGroup withOption(Option option, Object value) {
        this.values.get(option.getController()).put(option, "" + value);
        return this;
    }

    public ShellResult create(String sudoPW) throws IOException {
        return this.create(sudoPW, values.keySet());
    }

    /**
     * Varargs-version of {@link CGroup#create(String, Collection)}
     */
    public ShellResult create(String sudoPW, Controller... controllers) throws IOException {
        return this.create(sudoPW, Arrays.asList(controllers));
    }

    /**
     * @param sudoPW
     * @param controllers
     * @return
     * @throws IOException
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

    public ShellResult classify(String sudoPW) throws IOException {
        return this.classify(sudoPW, values.keySet());
    }

    public ShellResult classify(String sudoPW, Controller... controllers) throws IOException {
        return this.classify(sudoPW, Arrays.asList(controllers));
    }

    public ShellResult classify(String sudoPW, Collection<Controller> controllers) throws IOException {
        Shell shell = new Shell().addSudoRightCommand(sudoPW)
                .addShellCommand(
                        new ShellCommand(Command.CGCLASSIFY, true)
                                .withArgs("-g", qualifyName(controllers, this.name), ThreadUtil.getThreadId())
                );
        return shell.execute();
    }

    public ShellResult delete(String sudoPW) throws IOException {
        return this.delete(sudoPW, this.values.keySet());
    }

    public ShellResult delete(String sudoPW, Controller... controllers) throws IOException {
        return this.delete(sudoPW, Arrays.asList(controllers));
    }

    private ShellResult delete(String sudoPW, Collection<Controller> controllers) throws IOException {
        Shell shell = new Shell().addSudoRightCommand(sudoPW)
                .addShellCommand(
                        new ShellCommand(Command.CGDELETE, true)
                                .withArgs("-g", qualifyName(controllers, this.name))
                );
        return shell.execute();
    }

    private String qualifyName(Collection<Controller> controllers, String name) {
        return String.format("%s:%s", controllers.stream().map(Objects::toString)
                .collect(Collectors.joining(",")), name);
    }
}
