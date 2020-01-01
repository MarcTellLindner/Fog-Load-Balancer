package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.unikassel.prediction.pyearth.Predictor;
import de.unikassel.schedule.Scheduler;
import de.unikassel.schedule.SimpleScheduler;
import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.util.serialization.RemoteCallable;
import de.unikassel.util.serialization.Serializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Distributes load between multiple {@link WorkerNode}-instances it is connected to.
 */
public class LoadBalancer implements AutoCloseable {

    private final Scheduler scheduler;
    private final Predictor inputToScorePredictor;
    private final Predictor scoreToResourcesPredictor;

    private final LinkedHashSet<InetSocketAddress> workerNodeAddresses;

    private final Kryo kryo;

    private final ScheduledExecutorService executorService;


    /**
     * Create a new {@link LoadBalancer} without predictors.
     */
    public LoadBalancer() {
        this(new SimpleScheduler(), x -> new double[1], x -> new double[1]);
    }

    /**
     * Create a new {@link LoadBalancer}.
     *
     * @param scheduler                 {@link Scheduler} used for load-balancing.
     * @param inputToScorePredictor     {@link Predictor} from input to scalar score.
     * @param scoreToResourcesPredictor {@link Predictor} from scalar score to resources.
     */
    public LoadBalancer(Scheduler scheduler, Predictor inputToScorePredictor, Predictor scoreToResourcesPredictor) {
        this.scheduler = scheduler;
        this.inputToScorePredictor = inputToScorePredictor;
        this.scoreToResourcesPredictor = scoreToResourcesPredictor;

        this.workerNodeAddresses = new LinkedHashSet<>();

        this.kryo = Serializer.setupKryoInstance();

        this.executorService = Executors.newScheduledThreadPool(20);
    }

    /**
     * Varargs-version of {@link LoadBalancer#addWorkerNodeAddresses(Collection)}.
     */
    public void addWorkerNodeAddresses(InetSocketAddress... addresses) {
        this.addWorkerNodeAddresses(Arrays.asList(addresses));
    }

    /**
     * Add one ore more new {@link WorkerNode}-instances to be used.
     *
     * @param addresses The new {@link WorkerNode}s addresses and ports.
     */
    public void addWorkerNodeAddresses(Collection<InetSocketAddress> addresses) {
        this.workerNodeAddresses.addAll(addresses);
    }

    /**
     * Execute the given task on one of the known {@link WorkerNode}-instances.
     *
     * @param callable The task to be executed remotely.
     * @param input    The values to use for the prediction of time and resources.
     * @param <T>      The type of the returned value.
     * @return A Future of the execution on the chosen {@link WorkerNode}.
     */
    public <T> Future<T> executeOnWorker(RemoteCallable<T> callable, double... input) {
        TaskPrediction<T> taskPrediction = scheduleTasks(callable, input);
        return this.executorService.schedule(() -> {
                    scheduler.started(taskPrediction);
                    try {
                        return executeOnSpecifiedWorker(taskPrediction.worker, taskPrediction.task);
                    } finally {
                        scheduler.finished(taskPrediction);
                    }
                },
                (long) (taskPrediction.time - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }

    private <T> TaskPrediction<T> scheduleTasks(RemoteCallable<T> callable, double[] input) {
        double[] score = this.inputToScorePredictor.predict(input);
        double[] timeAndResources = this.scoreToResourcesPredictor.predict(score);

        double timePrediction = timeAndResources[0];
        double[] resourcePrediction = Arrays.copyOfRange(timeAndResources, 1, timeAndResources.length);

        return this.scheduler.schedule(callable, timePrediction, resourcePrediction, this.workerNodeAddresses);
    }

    private <T> T executeOnSpecifiedWorker(InetSocketAddress chosenAddress, RemoteCallable<T> callable)
            throws IOException {
        try (
                Socket worker = new Socket(chosenAddress.getAddress(), chosenAddress.getPort());
                Output out = new Output(worker.getOutputStream());
                Input in = new Input(worker.getInputStream())
        ) {
            kryo.writeClassAndObject(out, callable);
            out.flush();

            @SuppressWarnings("unchecked")
            T result = (T) kryo.readClassAndObject(in);
            return result;

        } catch (IOException e) {
            throw new IOException("Exception while executing on worker", e);
        }
    }

    @Override
    public void close() {
        this.executorService.shutdown();
    }

    public static void main(String... args) {

        try(LoadBalancer loadBalancer = new LoadBalancer()) {
            loadBalancer.addWorkerNodeAddresses(
                    new InetSocketAddress(InetAddress.getLocalHost(), WorkerNode.DEFAULT_RPC_PORT));
            String testString = "SUCCESS";
            String anonymous = loadBalancer.executeOnWorker(new RemoteCallable<String>() {
                @Override
                public String call() {
                    return testString;
                }
            }).get();
            String lambda = loadBalancer.executeOnWorker(() -> testString).get();

            System.out.printf("Anonymous:\t%s %nLambda:\t\t%s", anonymous, lambda);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
