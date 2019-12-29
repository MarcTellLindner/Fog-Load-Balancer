package de.unikassel.prediction.pyearth;

/**
 * Interface to predict values.
 */
public interface Predictor {
    /**
     * Predict values based on the input.
     *
     * @param x Input.
     * @return Prediction.
     */
    double[] predict(double[] x);
}