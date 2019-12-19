package de.unikassel.util;

import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;

import javax.script.ScriptEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PyEarth {
    public static Function<double[], double[]> trainEarthModel(double[][] x, double[][] y) throws IOException {
        Shell shell = new Shell();
        shell.addShellCommand(new ShellCommand("python3 python/earth.py", false).withArgs(
                Arrays.stream(x).map(row ->
                        Arrays.stream(row)
                                .mapToObj(Objects::toString)
                                .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";", "\"", "\"")),

                Arrays.stream(y).map(row ->
                        Arrays.stream(row)
                                .mapToObj(Objects::toString)
                                .collect(Collectors.joining(",")))
                        .collect(Collectors.joining(";", "\"", "\""))
        ));
        ShellResult result = shell.execute();
        if (result.exitVal != 0) {
            throw new IOException("Exception while training model: " + String.join("\n", result.err));
        }
        String formula = result.out.get(0);
        return createFunction(formula);
    }

    private static Function<double[], double[]> createFunction(String formula) {
        
        System.out.println(formula);
        ArrayList<Function<double[], double[]>> element;
        for(String part : formula.split("\\+")) {
            String[] sides = part.split("\\*", 2);
            double factor = Double.parseDouble(sides[1]);

            switch(sides[0].charAt(0)) {
                case '1': // Constant value

                    break;
                case 'x': // Linear value
                    break;
                case 'h': // Hinge function
                    break;
                default:
                    throw new IllegalStateException("Formula can't be parsed: " + part);
            }


        }

        return null;
    }

    public static void main(String... args) throws IOException {
        trainEarthModel(new double[][]{{1.0, 1.0}, {2.0, 3.0}}, new double[][]{{2.0}, {5.0}});
    }
}
