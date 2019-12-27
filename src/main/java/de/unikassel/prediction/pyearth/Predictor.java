package de.unikassel.prediction.pyearth;

public interface Predictor {
    double[] predict(double[] x);
}