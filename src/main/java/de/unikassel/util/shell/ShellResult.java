package de.unikassel.util.shell;

import java.util.List;

public class ShellResult {
    public final int exitVal;
    public final List<String> out;
    public final List<String> err;

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
