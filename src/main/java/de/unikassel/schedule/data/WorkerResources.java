package de.unikassel.schedule.data;

import java.net.InetSocketAddress;

public class WorkerResources {
    public final double timestamp;
    public final InetSocketAddress workerAddress;
    public final double[] resources;
    public final TaskPrediction<?> taskPrediction;

    public WorkerResources(double timestamp, InetSocketAddress workerAddress, double[] resources, TaskPrediction<?> taskPrediction) {
        this.timestamp = timestamp;
        this.workerAddress = workerAddress;
        this.resources = resources;
        this.taskPrediction = taskPrediction;
    }
}
