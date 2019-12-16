package de.unikassel.util.metrics;

import com.googlecode.mobilityrpc.MobilityRPC;
import com.googlecode.mobilityrpc.controller.MobilityController;
import com.googlecode.mobilityrpc.network.ConnectionId;
import com.googlecode.mobilityrpc.session.MobilitySession;
import de.unikassel.util.metrics.MetricData;
import de.unikassel.util.metrics.MetricType;
import de.unikassel.util.metrics.MetricsParser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class MonitoredSession implements Closeable {

    private static final int RPC_PORT = 5739;
    private static final int MONITORING_PORT = 8888;

    private final MobilityController mobilityController = MobilityRPC.newController();
    private final MobilitySession mobilitySession = mobilityController.newSession();
    private final ConnectionId rpcId;
    private final URL monitoringUrl;

    public MonitoredSession(String address) {
        this.rpcId = new ConnectionId(address, RPC_PORT);
        try {
            this.monitoringUrl = new URL(String.format("http://%s:%d/metrics", address, MONITORING_PORT));
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL was not valid");
        }
    }

    public void execute(Runnable runnable) {
        System.err.println(getMetrics().get(MetricType.PROCESS_CPU_USAGE).iterator().next().value);
        mobilitySession.execute(rpcId, runnable);
    }

    public <T> T execute(Callable<T> callable) {
        System.err.println(getMetrics().get(MetricType.PROCESS_CPU_USAGE).iterator().next().value);
        return mobilitySession.execute(rpcId, callable);
    }

    @Override
    public void close() {
        mobilitySession.release();
        mobilityController.destroy();
    }

    private HashMap<MetricType, HashSet<MetricData>> getMetrics() {
        try {
            HttpURLConnection connection = (HttpURLConnection) monitoringUrl.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder text = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            reader.close();

            MetricsParser parser = new MetricsParser();

            return parser.parse(text.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
