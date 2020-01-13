package de.unikassel.schedule;

import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Set;

public class SimpleScheduler implements Scheduler {

    ArrayList<TaskPrediction<?>> tasks = new ArrayList<>();

    @Override
    public synchronized <T> TaskPrediction<T> schedule(RemoteCallable<T> task, double timePrediction,
                                                       double[] resourcePrediction, Set<InetSocketAddress> workers) {
        TaskPrediction<?> lastTask = tasks.isEmpty() ? null : tasks.get(tasks.size() - 1);
        long executeAfter = lastTask == null
                ? Long.MIN_VALUE
                : (long) (lastTask.time + lastTask.duration);
        long startTime = Math.max(executeAfter, System.nanoTime());
        TaskPrediction<T> taskPrediction = new TaskPrediction<>(task, startTime,
                timePrediction, workers.iterator().next(), resourcePrediction, lastTask);
        tasks.add(taskPrediction);
        return taskPrediction;
    }

    @Override
    public void finished(TaskPrediction<?> taskPrediction) {
        tasks.remove(taskPrediction);
    }
}
