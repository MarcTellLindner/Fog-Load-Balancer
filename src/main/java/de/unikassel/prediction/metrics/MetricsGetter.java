package de.unikassel.prediction.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

/**
 * Class for requesting monitored metrics-data from a {@link de.unikassel.WorkerNode}.
 */
public class MetricsGetter {
    private final URL url;

    private final ArrayList<HashMap<MetricType, HashSet<MetricData>>> data;
    private final Thread updateThread;

    /**
     * Create new metrics-getter, connected to the specified address.
     *
     * @param workerNode The address of the {@link de.unikassel.WorkerNode} to connect to-
     * @param frequency  The frequency to request data with in milliseconds.
     */
    public MetricsGetter(InetSocketAddress workerNode, long frequency) {
        try {
            this.url = new URL("http", workerNode.getHostString(), workerNode.getPort(), "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // Should not occur, since http will always be a known protocol
        }
        this.data = new ArrayList<>();

        this.updateThread = new Thread(() -> {
            ArrayList<HashMap<MetricType, HashSet<MetricData>>> threadData = new ArrayList<>();
            try {
                MetricsParser parser = new MetricsParser();
                while (!Thread.currentThread().isInterrupted()) {
                    threadData.add(parser.parse(get()));
                    try {
                        Thread.sleep(frequency);
                    } catch (InterruptedException ignored) {
                        break; // The thread was interrupted while waiting => Stop the loop
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                data.addAll(threadData);
            }
        });
    }

    /**
     * Start requesting the metrics.
     */
    public synchronized void start() {
        updateThread.start();
    }

    /**
     * Stop requesting the metrics.
     *
     * @return The collected data.
     */
    public synchronized List<HashMap<MetricType, HashSet<MetricData>>> stop() {
        updateThread.interrupt();
        try {
            updateThread.join(100);
            return data;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return data;
        }
    }

    private synchronized String[] get() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))
        ) {
            String[] response = reader.lines().toArray(String[]::new);
            connection.disconnect();
            return response;
        }
    }

    /**
     * Get the metrics of a {@link de.unikassel.WorkerNode} once and blocking.
     *
     * @param workerNode The address to request the metrics at.
     * @return The collected metrics.
     * @throws IOException In case of connection problems.
     */
    public static HashMap<MetricType, HashSet<MetricData>> getMetrics(InetSocketAddress workerNode) throws IOException {
        MetricsGetter singleUseGetter = new MetricsGetter(workerNode, Long.MAX_VALUE);
        return new MetricsParser().parse(singleUseGetter.get());
    }
}
