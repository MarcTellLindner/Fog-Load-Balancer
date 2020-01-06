package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.CGroupBuilder;
import de.unikassel.cgroup.Controller;
import de.unikassel.cgroup.options.Cpu;
import de.unikassel.cgroup.options.Memory;
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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Distributes load between multiple {@link WorkerNode}-instances it is connected to.
 */
public class LoadBalancer implements AutoCloseable {

    private final Scheduler scheduler;
    private final Predictor inputToTaskSizePredictor;
    private final Predictor taskSizeToResourcesPredictor;
    private final CGroupBuilder cGroupBuilder;

    private final LinkedHashMap<InetSocketAddress, String> workerNodeAddresses;

    private final Kryo kryo;

    private final ScheduledExecutorService executorService;


    /**
     * Create a new {@link LoadBalancer} without predictors.
     */
    public LoadBalancer() {
        this(new SimpleScheduler(), x -> new double[1], x -> new double[1], x -> null);
    }

    /**
     * Create a new {@link LoadBalancer}.
     *
     * @param scheduler                    {@link Scheduler} used for load-balancing.
     * @param inputToTaskSizePredictor     {@link Predictor} from input to scalar score.
     * @param taskSizeToResourcesPredictor {@link Predictor} from scalar score to resources.
     * @param cGroupBuilder                {@link CGroupBuilder} to generate {@link CGroup} for executed task.
     */
    public LoadBalancer(Scheduler scheduler, Predictor inputToTaskSizePredictor, Predictor taskSizeToResourcesPredictor,
                        CGroupBuilder cGroupBuilder) {
        this.scheduler = scheduler;
        this.inputToTaskSizePredictor = inputToTaskSizePredictor;
        this.taskSizeToResourcesPredictor = taskSizeToResourcesPredictor;
        this.cGroupBuilder = cGroupBuilder;

        this.workerNodeAddresses = new LinkedHashMap<>();

        this.kryo = Serializer.setupKryoInstance();

        this.executorService = Executors.newScheduledThreadPool(20);
    }

    /**
     * Add one new {@link WorkerNode}-instance  and its password.
     *
     * @param address  The new {@link WorkerNode}s address and port.
     * @param password The new {@link WorkerNode}s password.
     */
    public void addWorkerNodeAddress(InetSocketAddress address, String password) {
        this.workerNodeAddresses.put(address, password);
    }

    /**
     * Add one or more new {@link WorkerNode}-instances  and their passwords.
     *
     * @param addresses The new {@link WorkerNode}s addresses, ports as keys and passwords as values.
     */
    public void addWorkerNodeAddresses(Map<InetSocketAddress, String> addresses) {
        this.workerNodeAddresses.putAll(addresses);
    }

    /**
     * Execute the given task on one of the known {@link WorkerNode}-instances.
     *
     * @param callable The task to be executed remotely.
     * @param input    The values to use for the prediction of time and resources.
     * @param <T>      The type of the returned value.
     * @return A Future of the execution on the chosen {@link WorkerNode}.
     * @throws IOException If the callable could not be scheduled by the scheduler.
     */
    public <T> Future<T> executeOnWorker(RemoteCallable<T> callable, double... input) throws IOException {
        TaskPrediction<T> taskPrediction = scheduleTask(callable, input);
        if (taskPrediction == null) {
            throw new IOException("Could not schedule task!");
        }
        return this.executorService.schedule(() -> {
                    scheduler.started(taskPrediction);
                    try {
                        return executeOnSpecifiedWorker(taskPrediction.worker, taskPrediction.task,
                                cGroupBuilder.buildCGroup(taskPrediction.resources));
                    } finally {
                        scheduler.finished(taskPrediction);
                    }
                },
                (long) (taskPrediction.time - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }

    private <T> TaskPrediction<T> scheduleTask(RemoteCallable<T> callable, double[] input) {
        double[] score = this.inputToTaskSizePredictor.predict(input);
        double[] timeAndResources = this.taskSizeToResourcesPredictor.predict(score);

        double timePrediction = timeAndResources[0];
        double[] resourcePrediction = Arrays.copyOfRange(timeAndResources, 1, timeAndResources.length);

        return this.scheduler.schedule(callable, timePrediction, resourcePrediction, this.workerNodeAddresses.keySet());
    }

    private <T> T executeOnSpecifiedWorker(InetSocketAddress chosenAddress, RemoteCallable<T> callable, CGroup cGroup)
            throws IOException {
        if (cGroup != null) {
            callable = this.wrapWithCGroup(callable, cGroup);
        }
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

    private <T> RemoteCallable<T> wrapWithCGroup(RemoteCallable<T> callable, CGroup cGroup) {
        return () -> {
            cGroup.create("");
            cGroup.classify("");
            T result = callable.call();
            cGroup.delete("");
            return result;
        };
    }

    @Override
    public void close() {
        this.executorService.shutdown();
    }
}
