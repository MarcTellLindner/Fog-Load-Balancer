package de.unikassel.prediction.pyearth;

import de.unikassel.prediction.Trainer;
import de.unikassel.util.shell.Shell;
import de.unikassel.util.shell.ShellCommand;
import de.unikassel.util.shell.ShellResult;
import org.codehaus.janino.ExpressionEvaluator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Train Predictor using PyEarth.
 */
public class PyEarth {

    private PyEarth() {
    }

    /**
     * Train a new {@link Predictor}.
     *
     * @param x Value of X for training.
     * @param y Value of y for training.
     * @param plot Plot the prediction if a value is provided.
     * @return TrainingResult representing the result of the training.
     * @throws IOException If a problems occurs while executing py-earth.
     */
    public static Trainer.TrainingResult trainEarthModel(double[][] x, double[][] y, Object... plot)
            throws IOException {
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
        ).withArgs(plot));
        ShellResult result = shell.execute();
        if (result.exitVal != 0) {
            throw new IOException("Exception while training model: " + String.join("\n", result.err));
        }

        return new Trainer.TrainingResult(
                readFormula(result.out.get(0)),
                Double.parseDouble(result.out.get(1))
        );
    }

    private static String readFormula(String result) {
        return result.replaceAll("\\[(.*)]", "$1")     // remove wrapping '[' and ']'
                .replaceAll("x(\\d+)", "x[$1]");       // variables to array-indexes;
    }

    /**
     * Compile a predictor from a formula.
     *
     * @param rmseFactor      Factor to multiply the RMSEs with.
     * @param trainingResults An {@link Trainer.TrainingResult}-instances containing formulas and RMSEs.
     * @return A compiled {@link Predictor}.
     * @throws IOException If an exception occurs during compilation.
     */
    public static Predictor compilePredictor(double rmseFactor, Trainer.TrainingResult... trainingResults)
            throws IOException {
        String javaFormula = Arrays.stream(trainingResults).map(
                res -> String.format(Locale.US, "(%s) + (%f * %f)",
                        res.formula, rmseFactor, res.rootMeanSquaredError
                )
        ).collect(Collectors.joining(" , ", "new double[]{", "}"));

        try {
            ExpressionEvaluator ee = new ExpressionEvaluator();
            ee.setDefaultImports("static java.lang.Math.*"); // Allow access to all functions in java.lang.Math
            ee.setNoPermissions();

            return (Predictor) ee.createFastEvaluator(
                    javaFormula,
                    Predictor.class, new String[]{"x"}
            );
        } catch (Exception e) {
            throw new IOException("Exception while reading trained model", e);
        }
    }
}
