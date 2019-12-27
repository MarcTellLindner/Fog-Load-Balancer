package de.unikassel.prediction;

import de.unikassel.LoadBalancer;
import de.unikassel.WorkerNode;
import de.unikassel.prediction.metrics.MetricData;
import de.unikassel.prediction.metrics.MetricType;
import de.unikassel.prediction.metrics.MetricsGetter;
import de.unikassel.prediction.metrics.MetricsParser;
import de.unikassel.util.serialization.RemoteCallable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Trainer {
    private final List<RemoteCallable<?>> trainingCalls;

    public Trainer(List<RemoteCallable<?>> trainingCalls) {
        this.trainingCalls = trainingCalls;
    }

    public double[][] train(String host, int rpcPort, int monitoringPort, MetricType... types) {

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.addWorkerNodeAddresses(new InetSocketAddress(host, rpcPort));

        ArrayList<Double> localMilliTime = new ArrayList<>();
        ArrayList<Double> remoteMilliTime = new ArrayList<>();
        LinkedHashMap<MetricType, ArrayList<Double>> metrics = new LinkedHashMap<>();
        for (MetricType type : types) {
            metrics.put(type, new ArrayList<>());
        }

        for (RemoteCallable<?> remoteCallable : trainingCalls) {
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
                loadBalancer.executeOnWorker(remoteCallable);
                rnt = System.nanoTime() - startTime;

                List<HashMap<MetricType, HashSet<MetricData>>> allValues = metricsGetter.stop();
                MetricsParser parser = new MetricsParser();
                for (MetricType type : types) {
                    data.put(type, parser.getMax(type, allValues).orElseThrow(IOException::new).value);
                }

            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            // Everything worked, so add the results
            localMilliTime.add(lnt / 1_000d);   // ns to ms
            remoteMilliTime.add(rnt / 1_000d);  // ns to ms
            metrics.forEach((k, v) -> v.add(data.get(k))); // Add the monitoring-data
        }

        return Stream.concat(
                Stream.of(localMilliTime, remoteMilliTime),
                metrics.values().stream()
        ).map(list -> list.stream().mapToDouble(Double::doubleValue).toArray())
                .toArray(double[][]::new);

    }

    public static void main(String[] args) {
        List<RemoteCallable<Long>> callables = LongStream.range(0L, 100L).map(l -> l * 1_000L)
                .mapToObj(l -> (RemoteCallable<Long>) () -> {
                    long sum = 0L;
                    for (long i = 0L; i < l; ++i) {
                        sum += i;
                    }
                    System.out.println("0 + ... + " + l + " = " + sum);
                    return sum;
                }).collect(Collectors.toList());

        Trainer trainer = new Trainer(Collections.unmodifiableList(callables));
        trainer.train("localhost", WorkerNode.DEFAULT_RPC_PORT, WorkerNode.DEFAULT_MONITORING_PORT,
                MetricType.PROCESS_CPU_USAGE, MetricType.JVM_MEMORY_USED);
    }
}
