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
import de.unikassel.schedule.data.ScheduledFuture;
import de.unikassel.schedule.data.WorkerResources;
import de.unikassel.util.serialization.RemoteCallable;
import org.junit.Test;
import test.util.complex.encrypt.AES;
import test.util.complex.sort.BubbleSort;
import test.util.complex.wait.WaitWithMemory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.unikassel.WorkerNode.DEFAULT_MONITORING_PORT;
import static de.unikassel.WorkerNode.DEFAULT_RPC_PORT;

public class LoadBalancerTest {

    private Random random = new Random();

    @Test
    public void sortingTest() throws IOException, InterruptedException {
        System.out.println("Sorting test");

        TaskGenerator<long[]> generator = (i, l) -> (() -> new BubbleSort().doSomethingComplex(i, l));

        PrimitiveIterator.OfInt ints = random.ints(10_000, 25_000).iterator();
        PrimitiveIterator.OfLong longs = random.longs(0, 100).iterator();

        test(
                generator,
                ints,
                longs,
                1_000,
                10, 50, 250, 1_000
        );
    }

    @Test
    public void encryptionTest() throws IOException, InterruptedException {
        System.out.println("Encryption test");

        TaskGenerator<byte[]> generator = (i, l) -> (() -> new AES().doSomethingComplex(i, l));

        PrimitiveIterator.OfInt ints = random.ints(10, 1_000).iterator();
        PrimitiveIterator.OfLong longs = random.longs(1_000, 1_000_000).iterator();

        test(
                generator,
                ints,
                longs,
                1_000,
                10, 50, 250, 1_000
        );
    }

    @Test
    public void waitWithMemoryTest() throws IOException, InterruptedException {
        System.out.println("Wait with memory test");

        TaskGenerator<Long> generator = (i, l) -> (() -> new WaitWithMemory().doSomethingComplex(i, l));

        PrimitiveIterator.OfInt ints = random.ints(1, 2_500).iterator();
        PrimitiveIterator.OfLong longs = random.longs(0, 1).iterator(); // Is ignored

        test(
                generator,
                ints,
                longs,
                1_000,
                10, 50, 250, 1_000
        );
    }

    @FunctionalInterface
    private interface TaskGenerator<T> {
        RemoteCallable<T> generate(int val1, long val2);
    }

    private <T> void test(TaskGenerator<T> generator, PrimitiveIterator.OfInt ints, PrimitiveIterator.OfLong longs,
                          int nTraining, int... nEvaluations) throws IOException, InterruptedException {

        RemoteCallable<?>[] trainingCalls = new RemoteCallable<?>[nTraining];
        double[][] trainingValues = new double[nTraining][];
        for (int i = 0; i < nTraining; ++i) {
            final int intVal = ints.nextInt();
            final long longVal = longs.nextLong();
            trainingCalls[i] = generator.generate(intVal, longVal);
            trainingValues[i] = new double[]{intVal, longVal};
        }

        Trainer trainer = createTrainer(trainingCalls, trainingValues);

        Scheduler smartScheduler = createScheduler();
        Scheduler basicScheduler = new SimpleScheduler();

        CGroupBuilder cGroupBuilder = createCGroupBuilder();

        System.out.printf("Finished training: (id = %d)%n" +
                        "\t Input to task size predictor:%n %s%n" +
                        "\t Task size to resource predictor:%n %s%n%n",
                trainer.hashCode(),
                trainer.getInputToTaskSizeFormula(),
                Arrays.toString(trainer.getTaskSizeToResourceFormula()));


        for (int nEvaluation : nEvaluations) {
            System.out.printf("n = %d%n", nEvaluation);
            for (Scheduler scheduler : new Scheduler[]{basicScheduler, smartScheduler}) {
                int exceptions = 0;
                try (
                        LoadBalancer loadBalancer = new LoadBalancer(scheduler,
                                trainer.getInputToTaskSizePredictor(), trainer.getTaskSizeToResourcePredictor(),
                                cGroupBuilder)
                ) {
                    loadBalancer.addWorkerNodeAddress(
                            new InetSocketAddress(System.getenv("worker"), DEFAULT_RPC_PORT),
                            System.getenv("password"));

                    List<ScheduledFuture<T>> futures = new ArrayList<>();

                    long tStart = System.currentTimeMillis();
                    for (int i = 0; i < nEvaluation; ++i) {
                        final int intVal = ints.nextInt();
                        final long longVal = longs.nextLong();

                        try {
                            futures.add(loadBalancer.executeOnWorker(
                                    generator.generate(intVal, longVal), intVal, longVal)
                            );
                        } catch (IOException e) {
                            ++exceptions;
                        }

                        Thread.sleep(10);
                    }
                    for (ScheduledFuture<T> future : futures) {
                        try {
                            future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            ++exceptions;
                        }

                    }

                    System.out.printf("Time: %.2f seconds%s%n",
                            (System.currentTimeMillis() - tStart) / 1_000.,
                            exceptions == 0 ? "" : String.format(" (%d exceptions)", exceptions)
                    );
                }
            }
            System.out.println();
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
                return new WorkerResources(System.nanoTime(), worker, freeResources, null);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    private CGroupBuilder createCGroupBuilder() {
        return predictions -> {
//            System.out.println("Predictions: " + Arrays.toString(predictions));
            return new CGroup(String.format("CG%d", Arrays.hashCode(predictions)), Controller.CPU, Controller.MEMORY)
                    .withOption(Cpu.SHARES, (int) (predictions[0] * 1024))
                    .withOption(Memory.LIMIT_IN_BYTES, (int) (predictions[1]));
        };
    }
}
