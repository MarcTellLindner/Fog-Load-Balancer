package de.unikassel.prediction.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class MetricsGetter {
    private final URL url;

    private final ArrayList<HashMap<MetricType, HashSet<MetricData>>> data;
    private final Thread updateThread;

    public MetricsGetter(InetSocketAddress workerNode, long frequency) throws MalformedURLException {
        this.url = new URL("http", workerNode.getHostString(), workerNode.getPort(), "/");
        this.data = new ArrayList<>();

        this.updateThread = new Thread(() -> {
            ArrayList<HashMap<MetricType, HashSet<MetricData>>> threadData = new ArrayList<>();
            try {
                MetricsParser parser = new MetricsParser();
                while (!Thread.currentThread().isInterrupted()) {
                    threadData.add(parser.parse(get()));
                    try {
                        Thread.sleep(frequency * 1_000);
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

    public void start() {
        updateThread.start();
    }

    public List<HashMap<MetricType, HashSet<MetricData>>> stop() {
        updateThread.interrupt();
        try {
            updateThread.join();
            return data;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String[] get() throws IOException {
        URLConnection connection = this.url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        return reader.lines().toArray(String[]::new);
    }
}
