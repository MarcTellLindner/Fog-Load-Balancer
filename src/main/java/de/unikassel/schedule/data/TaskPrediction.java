package de.unikassel.schedule.data;

import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;

public class TaskPrediction<T> {
    public final RemoteCallable<T> task;
    public final double time;
    public final InetSocketAddress worker;
    public final double[] resources;

    public TaskPrediction(RemoteCallable<T> task, double time, InetSocketAddress worker, double[] resources) {
        this.task = task;
        this.time = time;
        this.worker = worker;
        this.resources = resources;
    }
}