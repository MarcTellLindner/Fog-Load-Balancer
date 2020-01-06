package de.unikassel;

import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.CGroupBuilder;
import de.unikassel.cgroup.Controller;
import de.unikassel.cgroup.options.Cpu;
import de.unikassel.cgroup.options.Memory;
import de.unikassel.prediction.Trainer;
import de.unikassel.prediction.metrics.MetricData;
import de.unikassel.prediction.metrics.MetricType;
import de.unikassel.prediction.metrics.MetricsGetter;
import de.unikassel.prediction.metrics.MetricsParser;
import de.unikassel.schedule.QueueScheduler;
import de.unikassel.schedule.Scheduler;
import de.unikassel.schedule.SimpleScheduler;
import de.unikassel.schedule.data.WorkerResources;
import de.unikassel.util.serialization.RemoteCallable;
import org.junit.Test;
import test.util.complex.encrypt.AES;
import test.util.complex.sort.BubbleSort;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.unikassel.WorkerNode.DEFAULT_MONITORING_PORT;
import static de.unikassel.WorkerNode.DEFAULT_RPC_PORT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LoadBalancerTest {

    @Test
    public void test() {

        try (LoadBalancer loadBalancer = new LoadBalancer(new SimpleScheduler(), x -> new double[1], x -> new double[1],
                predictions -> new CGroup("Test", Controller.CPU, Controller.MEMORY)
                        .withOption(Cpu.SHARES, 1024)
                        .withOption(Memory.LIMIT_IN_BYTES, 1024))
        ) {
            loadBalancer.addWorkerNodeAddress(
                    new InetSocketAddress(System.getenv("worker"), DEFAULT_RPC_PORT),
                    System.getenv("password"));
            String testString = "SUCCESS";
            String anonymous = loadBalancer.executeOnWorker(new RemoteCallable<String>() {
                @Override
                public String call() {
                    return testString;
                }
            }).get();
            String lambda = loadBalancer.executeOnWorker(() -> testString).get();

            assertEquals(testString, anonymous);
            assertEquals(testString, lambda);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sortingTest() throws IOException, ExecutionException, InterruptedException {
        int trainingExamples = 100;

        RemoteCallable<?>[] trainingCalls = new RemoteCallable<?>[trainingExamples];
        double[][] trainingValues = new double[trainingExamples][1];
        for (int i = 0; i < trainingExamples; ++i) {
            final int size = (i + 1) * 10;
            trainingCalls[i] = () -> new BubbleSort().doSomethingComplex(size, size);
            trainingValues[i][0] = size;
        }

        Trainer trainer = createTrainer(trainingCalls, trainingValues);

        Scheduler scheduler = createScheduler();

        CGroupBuilder cGroupBuilder = createCGroupBuilder();

        try (
                LoadBalancer loadBalancer = new LoadBalancer(scheduler,
                        trainer.getInputToTaskSizePredictor(), trainer.getTaskSizeToResourcePredictor(),
                        cGroupBuilder)
        ) {
            loadBalancer.addWorkerNodeAddress(
                    new InetSocketAddress(System.getenv("worker"), DEFAULT_RPC_PORT),
                    System.getenv("password"));
            long[] result = loadBalancer.executeOnWorker(
                    () -> new BubbleSort().doSomethingComplex(500, 500L), 500
            ).get();

            long[] expected = result.clone();
            Arrays.sort(expected);

            assertArrayEquals(expected, result);
        }
    }

    @Test
    public void encryptionTest() throws IOException, ExecutionException, InterruptedException {
        int trainingExamples = 100;

        RemoteCallable<?>[] trainingCalls = new RemoteCallable<?>[trainingExamples];
        double[][] trainingValues = new double[trainingExamples][1];
        for (int i = 0; i < trainingExamples; ++i) {
            final int size = (i + 1) * 10;
            trainingCalls[i] = () -> new AES().doSomethingComplex(size, size);
            trainingValues[i][0] = size;
        }

        Trainer trainer = createTrainer(trainingCalls, trainingValues);

        Scheduler scheduler = createScheduler();

        CGroupBuilder cGroupBuilder = createCGroupBuilder();

        try (
                LoadBalancer loadBalancer = new LoadBalancer(scheduler,
                        trainer.getInputToTaskSizePredictor(), trainer.getTaskSizeToResourcePredictor(),
                        cGroupBuilder)
        ) { loadBalancer.addWorkerNodeAddress(
                new InetSocketAddress(System.getenv("worker"), DEFAULT_RPC_PORT),
                System.getenv("password"));
            byte[] result = loadBalancer.executeOnWorker(
                    () -> new AES().doSomethingComplex(500, 500L), 500
            ).get();
        }

    }

    private Trainer createTrainer(RemoteCallable<?>[] trainingCalls, double[][] trainingValues) throws IOException {
        Trainer trainer = new Trainer(trainingCalls, trainingValues);
        return trainer.measure(System.getenv("worker"),
                DEFAULT_RPC_PORT, DEFAULT_MONITORING_PORT,
                System.getenv(("password")),
                MetricType.PROCESS_CPU_USAGE,
                MetricType.JVM_MEMORY_USED)
                .train();
    }

    private QueueScheduler createScheduler() {
        return new QueueScheduler(worker -> {
            try {
                List<HashMap<MetricType, HashSet<MetricData>>> metrics
                        = Collections.singletonList(MetricsGetter.getMetrics(
                        new InetSocketAddress(worker.getAddress(), DEFAULT_MONITORING_PORT)));
                MetricsParser parser = new MetricsParser();
                // 1. - used
                double cpuFree = parser.getMax(MetricType.PROCESS_CPU_USAGE, metrics, true)
                        .map(m -> 1. - m.value).orElseThrow(IOException::new);

                // Max - used
                double memFree = parser.getMax(MetricType.JVM_MEMORY_MAX, metrics, true)
                        .map(m -> m.value).orElseThrow(IOException::new)
                        -
                        parser.getMax(MetricType.JVM_MEMORY_USED, metrics, true)
                                .map(m -> m.value).orElseThrow(IOException::new);

                double[] freeResources = {cpuFree, memFree};
                return new WorkerResources(System.currentTimeMillis(), worker, freeResources);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    private CGroupBuilder createCGroupBuilder() {
        return predictions ->
                new CGroup(String.format("CG%d", Arrays.hashCode(predictions)), Controller.CPU, Controller.MEMORY)
                        .withOption(Cpu.SHARES, (int) (predictions[0] * 1024))
                        .withOption(Memory.LIMIT_IN_BYTES, (int) (predictions[1]));
    }
}
