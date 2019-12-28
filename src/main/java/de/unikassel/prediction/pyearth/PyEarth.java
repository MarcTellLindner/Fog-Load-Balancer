package de.unikassel.prediction.pyearth;

import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;
import org.codehaus.janino.ExpressionEvaluator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class PyEarth {

    private PyEarth() {
    }

    public static Predictor trainEarthModel(double[][] x, double[][] y) throws IOException {
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
        String formula = readFormula(result.out.get(0));
        return createFunction(formula);
    }

    private static String readFormula(String result) {
        return String.format("new double[]{%s}",               // Wrap in array-creation
                result.replaceAll("\\[(.*)]", "$1")     // remove wrapping '[' and ']'
                        .replaceAll("x(\\d+)", "x[$1]") // variables to array-indexes;
        );
    }

    private static Predictor createFunction(String formula) throws IOException {
        try {
            ExpressionEvaluator ee = new ExpressionEvaluator();
            ee.setDefaultImports("static java.lang.Math.*"); // Allow access to all functions in java.lang.Math
            ee.setNoPermissions();
            return (Predictor) ee.createFastEvaluator(
                    formula,
                    Predictor.class, new String[]{"x"}
            );
        } catch (Exception e) {
            throw new IOException("Exception while reading trained model", e);
        }
    }
}
