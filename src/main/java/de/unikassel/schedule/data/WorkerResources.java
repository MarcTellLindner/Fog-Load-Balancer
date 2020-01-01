package de.unikassel.schedule.data;

import java.net.InetSocketAddress;

public class WorkerResources {
    public final double timestamp;
    public final InetSocketAddress workerAddress;
    public final double[] freeResources;

    public WorkerResources(double timestamp, InetSocketAddress workerAddress, double[] freeResources) {
        this.timestamp = timestamp;
        this.workerAddress = workerAddress;
        this.freeResources = freeResources;
    }
}
