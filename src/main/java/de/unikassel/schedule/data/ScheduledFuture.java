package de.unikassel.schedule.data;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScheduledFuture <T> implements Future<T> {

    private final Future<T> innerFuture;
    private final TaskPrediction<T> taskPrediction;

    public ScheduledFuture(Future<T> innerFuture, TaskPrediction<T> taskPrediction) {
        this.innerFuture = innerFuture;
        this.taskPrediction = taskPrediction;
    }

    public TaskPrediction<T> getTaskPrediction() {
        return taskPrediction;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return innerFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return innerFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return innerFuture.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return innerFuture.get(timeout, unit);
    }
}
