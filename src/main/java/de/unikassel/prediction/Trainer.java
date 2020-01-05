package de.unikassel.prediction;

import de.unikassel.LoadBalancer;
import de.unikassel.prediction.metrics.MetricData;
import de.unikassel.prediction.metrics.MetricType;
import de.unikassel.prediction.metrics.MetricsGetter;
import de.unikassel.prediction.metrics.MetricsParser;
import de.unikassel.prediction.pyearth.Predictor;
import de.unikassel.prediction.pyearth.PyEarth;
import de.unikassel.util.serialization.RemoteCallable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for training a model to predict required resources of tasks.
 */
public class Trainer {
    private final RemoteCallable<?>[] trainingCalls;
    private final double[][] trainingValues;

    private final ArrayList<Double> localMilliTime;
    private final ArrayList<Double> remoteMilliTime;
    private final LinkedHashMap<MetricType, ArrayList<Double>> metrics;
    private final ArrayList<double[]> relevantTrainingValues;

    private Predictor inputToTaskSizePredictor;
    private Predictor taskSizeToResourcePredictor;

    public Trainer(RemoteCallable<?>[] trainingCalls, double[][] trainingValues) {
        this.trainingCalls = trainingCalls;
        this.trainingValues = trainingValues;
        this.localMilliTime = new ArrayList<>();
        this.remoteMilliTime = new ArrayList<>();
        this.metrics = new LinkedHashMap<>();
        this.relevantTrainingValues = new ArrayList<>();
    }

    /**
     * Measure metrics and time for the training calls.
     *
     * <p>The training calls will be executed once locally and once on the specified worker node.</p>
     *
     * @param host           The address of the worker node.
     * @param rpcPort        The port to send the calls to.
     * @param monitoringPort The port to monitor the worker node.
     * @param password       The password of the worker node.
     * @param types          The additional metrics to measure.
     * @return The trainer this method was called on.
     */
    private Trainer measure(String host, int rpcPort, int monitoringPort, String password, MetricType... types) {

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.addWorkerNodeAddress(new InetSocketAddress(host, rpcPort), password);

        for (MetricType type : types) {
            metrics.put(type, new ArrayList<>());
        }

        for (int i = 0; i < this.trainingCalls.length; ++i) {

            RemoteCallable<?> remoteCallable = this.trainingCalls[i];
            double[] value = this.trainingValues[i];

            // Call locally and measure time:
            long lnt;
            try {
                long startTime = System.nanoTime();
                remoteCallable.call();
                lnt = System.nanoTime() - startTime;
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            // Call remotely and measure values:
            long rnt;
            HashMap<MetricType, Double> data = new HashMap<>();
            try {
                MetricsGetter metricsGetter = new MetricsGetter(new InetSocketAddress(host, monitoringPort), 1);
                metricsGetter.start();

                long startTime = System.nanoTime(); // Network time included at the moment
                loadBalancer.executeOnWorker(remoteCallable).get();
                rnt = System.nanoTime() - startTime;

                List<HashMap<MetricType, HashSet<MetricData>>> allValues = metricsGetter.stop();
                MetricsParser parser = new MetricsParser();
                for (MetricType type : types) {
                    data.put(type, parser.getMax(type, allValues, true).orElseThrow(IOException::new).value);
                }

            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
                continue;
            }

            // Everything worked, so add the results
            localMilliTime.add(lnt / 1_000d);   // ns to ms
            remoteMilliTime.add(rnt / 1_000d);  // ns to ms
            metrics.forEach((k, v) -> v.add(data.get(k))); // Add the monitoring-data
            this.relevantTrainingValues.add(value); // Add the training value to be considered
        }

        return this;
    }

    /**
     * Train {@link Predictor}s based on measured values.
     *
     * <p>The method {@link Trainer#measure(String, int, int, String, MetricType...)}
     * has to be called before calling this method.</p>
     *
     * @return The trainer this method was called on.
     * @throws IOException If problems occur during training.
     */
    public Trainer train() throws IOException {

        double[][] values = relevantTrainingValues.toArray(new double[0][]);
        double[][] scores = localMilliTime.stream().map(d -> new double[]{d}).toArray(double[][]::new);
        double[][] resources = IntStream.range(0, remoteMilliTime.size())
                .mapToObj(
                        i -> Stream.concat(Stream.of(remoteMilliTime), metrics.values().stream())
                                .mapToDouble(list -> list.get(i)).toArray()
                ).toArray(double[][]::new);

        this.inputToTaskSizePredictor = PyEarth.trainEarthModel(values, scores);
        this.taskSizeToResourcePredictor = PyEarth.trainEarthModel(scores, resources);

        return this;
    }

    /**
     * Get the inputToTaskSizePredictor, if the {@link Trainer#train()}-method has already been called. Null otherwise.
     *
     * @return The trained inputToTaskSizePredictor or null.
     */
    public Predictor getInputToTaskSizePredictor() {
        return inputToTaskSizePredictor;
    }

    /**
     * Get the taskSizeToResourcePredictor, if the {@link Trainer#train()}-method has already been called. Null otherwise.
     *
     * @return The trained taskSizeToResourcePredictor or null.
     */
    public Predictor getTaskSizeToResourcePredictor() {
        return taskSizeToResourcePredictor;
    }
}