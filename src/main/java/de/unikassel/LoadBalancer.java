package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.CGroupBuilder;
import de.unikassel.prediction.pyearth.Predictor;
import de.unikassel.schedule.Scheduler;
import de.unikassel.schedule.SimpleScheduler;
import de.unikassel.schedule.data.ScheduledFuture;
import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.util.serialization.RemoteCallable;
import de.unikassel.util.serialization.Serializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * Distributes load between multiple {@link WorkerNode}-instances it is connected to.
 */
public class LoadBalancer implements AutoCloseable {

    private final Scheduler scheduler;
    private final Predictor inputToTaskSizePredictor;
    private final Predictor taskSizeToResourcesPredictor;
    private final CGroupBuilder cGroupBuilder;

    private final LinkedHashMap<InetSocketAddress, String> workerNodeAddresses;

    private final ExecutorService executorService;
    private final HashMap<TaskPrediction<?>, HashSet<RunnableFuture<?>>> waiting;


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

        this.executorService = Executors.newCachedThreadPool();
        waiting = new HashMap<>();
    }

    /**
     * Add one new {@link WorkerNode}-instance  and its password.
     *
     * @param address  The new {@link WorkerNode}s address and port.
     * @param password The new {@link WorkerNode}s password.
     */
    public synchronized void addWorkerNodeAddress(InetSocketAddress address, String password) {
        this.workerNodeAddresses.put(address, password);
    }

    /**
     * Add one or more new {@link WorkerNode}-instances  and their passwords.
     *
     * @param addresses The new {@link WorkerNode}s addresses, ports as keys and passwords as values.
     */
    public synchronized void addWorkerNodeAddresses(Map<InetSocketAddress, String> addresses) {
        this.workerNodeAddresses.putAll(addresses);
    }

    /**
     * Execute the given task on one of the known {@link WorkerNode}-instances.
     *
     * @param callable The task to be executed remotely.
     * @param input    The values to use for the prediction of time and resources.
     * @param <T>      The type of the returned value.
     * @return A {@link ScheduledFuture} of the execution on the chosen {@link WorkerNode}.
     * @throws IOException If the callable could not be scheduled by the scheduler.
     */
    public synchronized <T> ScheduledFuture<T> executeOnWorker(RemoteCallable<T> callable, double... input) throws IOException {
        TaskPrediction<T> taskPrediction = scheduleTask(callable, input);
        if (taskPrediction == null) {
            throw new IOException("Could not schedule task!");
        }

        waiting.put(taskPrediction, new HashSet<>());

        RunnableFuture<T> future = new FutureTask<>(() -> {

            scheduler.started(taskPrediction);
            try {
                return executeOnSpecifiedWorker(taskPrediction.worker, taskPrediction.task,
                        cGroupBuilder.buildCGroup(taskPrediction.resources));
            } finally {
                startWaitingAfter(taskPrediction);
            }
        });

        if (taskPrediction.startAfter == null || !waiting.containsKey(taskPrediction.startAfter)) {
            // Start the task immediately
            executorService.submit(future);
        } else {
            // Wait for other task
            waiting.get(taskPrediction.startAfter).add(future);
        }

        return new ScheduledFuture<>(future, taskPrediction);
    }

    private synchronized void startWaitingAfter(TaskPrediction<?> taskPrediction) {
        for (RunnableFuture<?> waitingFuture : this.waiting.get(taskPrediction)) {
            executorService.submit(waitingFuture);
        }
//        System.out.println("\tREMOVED");
        this.waiting.remove(taskPrediction);
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
            Kryo kryo = Serializer.setupKryoInstance();
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
    public synchronized void close() {
        this.executorService.shutdown();
    }
}
