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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import test.util.complex.encrypt.AES;
import test.util.complex.sort.BubbleSort;
import test.util.complex.wait.WaitWithMemory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.unikassel.WorkerNode.DEFAULT_MONITORING_PORT;
import static de.unikassel.WorkerNode.DEFAULT_RPC_PORT;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoadBalancerTest {

    private Random random = new Random();
    private String[] workers = System.getenv("workers").split(" ");
    private String[] passwords = System.getenv("passwords").split(" ");

    @Test
    public void sortingTest() throws IOException, InterruptedException {
        System.out.println("Sorting test");

        TaskGenerator generator = (i, l) -> (() -> new BubbleSort().doSomethingComplex(i, l));

        PrimitiveIterator.OfInt ints = random.ints(1_000, 2_500).iterator();
        PrimitiveIterator.OfLong longs = random.longs(0, Long.MAX_VALUE).iterator();

        test(
                generator,
                ints,
                longs,
                1_000,
                new int[]{1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000},
                new int[]{1_000, 1_000 / 2, 1_000 / 3, (int) (1_000 / 4.5),
                        1_000 / 6, 1_000 / 9, 1_000 / 12, 1_000 / 15,
                        1_000 / 18, 1_000 / 21, 1_000 / 24, 1_000 / 27}
        );
    }

    @Test
    public void encryptionTest() throws IOException, InterruptedException {
        System.out.println("Encryption test");

        TaskGenerator generator = (i, l) -> (() -> new AES().doSomethingComplex(i, l));

        PrimitiveIterator.OfInt ints = random.ints(10, 1_000).iterator();
        PrimitiveIterator.OfLong longs = random.longs(1_000, 1_000_000).iterator();

        test(
                generator,
                ints,
                longs,
                1_000,
                new int[]{1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000},
                new int[]{
                        (int) (1_000 / 0.25), (int) (1_000 / 0.5), (int) (1_000 / 1.), (int) (1_000 / 1.5),
                        (int) (1_000 / 3.0), (int) (1_000 / 4.5), (int) (1_000 / 6.0), (int) (1_000 / 7.5),
                        (int) (1_000 / 9.0), (int) (1_000 / 10.5), (int) (1_000 / 12.0), (int) (1_000 / 13.5)
                }
        );
    }

    @Test
    public void waitWithMemoryTest() throws IOException, InterruptedException {
        System.out.println("Wait with memory test");

        TaskGenerator generator = (i, l) -> (() -> new WaitWithMemory().doSomethingComplex(i, l));

        PrimitiveIterator.OfInt ints = random.ints(1, 1_000).iterator();
        PrimitiveIterator.OfLong longs = random.longs(0, 1).iterator(); // Is ignored

        test(
                generator,
                ints,
                longs,
                1_000,
                new int[]{1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000, 1_000},
                new int[]{
                        (int) (1_000 / 0.25), (int) (1_000 / 0.5), (int) (1_000 / 1.), (int) (1_000 / 1.5),
                        (int) (1_000 / 3.0), (int) (1_000 / 4.5), (int) (1_000 / 6.0), (int) (1_000 / 7.5),
                        (int) (1_000 / 9.0), (int) (1_000 / 10.5), (int) (1_000 / 12.0), (int) (1_000 / 13.5)
                }
        );
    }

    @FunctionalInterface
    private interface TaskGenerator {
        RemoteCallable<Boolean> generate(int val1, long val2);
    }

    private void test(TaskGenerator generator, PrimitiveIterator.OfInt ints, PrimitiveIterator.OfLong longs,
                      int nTraining, int[] nEvaluations, int[] waits) throws IOException, InterruptedException {

        if (nEvaluations.length != waits.length) {
            throw new IllegalStateException("nEvaluation and waits must have the same length");
        }

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


        for (int i = 0; i < nEvaluations.length; ++i) {
            int nEvaluation = nEvaluations[i];
            int wait = waits[i];
            System.out.printf("n = %d | delta_t = %d%n", nEvaluation, wait);
            for (Scheduler scheduler : new Scheduler[]{basicScheduler, smartScheduler}) {
                int exceptions = 0;
                try (
                        LoadBalancer loadBalancer = new LoadBalancer(scheduler,
                                trainer.getInputToTaskSizePredictor(), trainer.getTaskSizeToResourcePredictor(),
                                cGroupBuilder)
                ) {
                    for (int j = 0; j < workers.length; ++j) {
                        loadBalancer.addWorkerNodeAddress(
                                new InetSocketAddress(workers[i], DEFAULT_RPC_PORT),
                                passwords[i]
                        );
                    }

                    List<ScheduledFuture<Boolean>> futures = new ArrayList<>();

                    long tStart = System.currentTimeMillis();
                    for (int j = 0; j < nEvaluation; ++j) {
                        final int intVal = ints.nextInt();
                        final long longVal = longs.nextLong();

                        try {
                            ScheduledFuture<Boolean> f = loadBalancer.executeOnWorker(
                                    generator.generate(intVal, longVal), intVal, longVal
                            );
                            futures.add(f);
                        } catch (IOException e) {
                            ++exceptions;
                        }

                        Thread.sleep(wait);
                    }

                    for (ScheduledFuture<Boolean> future : futures) {
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

                    double avgRetentionTime
                            = futures.stream().mapToLong(sf -> sf.getExecutionTimes().retention())
                            .average().orElse(-1);

                    double avgWaitingTime
                            = futures.stream().mapToLong(sf -> sf.getExecutionTimes().waited())
                            .average().orElse(-1);

                    double avgProcessingTime
                            = futures.stream().mapToLong(sf -> sf.getExecutionTimes().processed())
                            .average().orElse(-1);

                    System.out.printf(
                            "\t Average retention time: %.5f ms" +
                                    "\t Average waiting time: %.5f ms" +
                                    "\t Average processing time: %.5f ms%n",
                            avgRetentionTime / 1_000_000,
                            avgWaitingTime / 1_000_000,
                            avgProcessingTime / 1_000_000);
                }
            }
            System.out.println();
        }
    }

    private Trainer createTrainer(RemoteCallable<?>[] trainingCalls, double[][] trainingValues) throws IOException {
        Trainer trainer = new Trainer(trainingCalls, trainingValues);
        return trainer.measure(workers[0],
                DEFAULT_RPC_PORT, DEFAULT_MONITORING_PORT,
                passwords[0],
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
                // 1.0 - used
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
            return new CGroup(String.format("CG%d", Arrays.hashCode(predictions)), Controller.CPU, Controller.MEMORY)
                    .withOption(Cpu.SHARES, (int) (predictions[0] * 1024))
                    .withOption(Memory.LIMIT_IN_BYTES, (int) (predictions[1]));
        };
    }
}
