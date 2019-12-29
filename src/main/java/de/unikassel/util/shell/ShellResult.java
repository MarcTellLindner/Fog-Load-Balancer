package de.unikassel.util.shell;

import java.util.List;

/**
 * The result of the execution of commands with a {@link Shell}.
 */
public class ShellResult {
    public final int exitVal;
    public final List<String> out;
    public final List<String> err;

    /**
     * New result with exitVal, out amd in.
     *
     * @param exitVal The exit-value of the execution of the {@link Shell}.
     * @param out The output to stdout of the {@link Shell}.
     * @param err The output to error of the {@link Shell}.
     */
    public ShellResult(int exitVal, List<String> out, List<String> err) {

        this.exitVal = exitVal;
        this.out = out;
        this.err = err;
    }

    @Override
    public String toString() {
        return String.format("out: %s; err: %s; exitVal: %d", out, err, exitVal);
    }
}
