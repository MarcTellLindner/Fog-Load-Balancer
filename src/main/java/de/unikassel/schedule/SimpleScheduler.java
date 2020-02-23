package de.unikassel.schedule;

import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;
import java.util.*;

public class SimpleScheduler implements Scheduler {

    private final HashMap<InetSocketAddress, TaskPrediction<?>> lastTaskPerWorker;

    public SimpleScheduler() {
        lastTaskPerWorker = new HashMap<>();
    }

    @Override
    public synchronized <T> TaskPrediction<T> schedule(RemoteCallable<T> task, double timePrediction,
                                                       double[] resourcePrediction, Set<InetSocketAddress> workers) {
        InetSocketAddress worker = null;
        double time = Double.MAX_VALUE;
        TaskPrediction<?> startAfter = null;

        if (timePrediction == -1) {
            if (workers.size() != 1) {
                throw new IllegalStateException("Cannot run without time prediction for more than one worker");
            }
            worker = workers.iterator().next();
            time = System.nanoTime();
            startAfter = lastTaskPerWorker.get(worker);
        } else {

            for (InetSocketAddress currentWorker : workers) {
                if (!lastTaskPerWorker.containsKey(currentWorker)) {
                    worker = currentWorker;
                    time = System.nanoTime();
                    break;
                }
            }

            if (worker == null) {
                for (InetSocketAddress currentWorker : workers) {
                    TaskPrediction<?> workerLastTask = lastTaskPerWorker.get(currentWorker);
                    double freeAt = workerLastTask.time + workerLastTask.duration;

                    if (freeAt < time) {
                        time = freeAt;
                        worker = currentWorker;
                        startAfter = workerLastTask;
                    }
                }
            }
        }
        TaskPrediction<T> taskPrediction = new TaskPrediction<>(
                task, time, timePrediction, worker, resourcePrediction, startAfter
        );
        lastTaskPerWorker.put(worker, taskPrediction);
        return taskPrediction;
    }

    @Override
    public synchronized void finished(TaskPrediction<?> taskPrediction) {
        lastTaskPerWorker.remove(taskPrediction.worker, taskPrediction);
    }
}
