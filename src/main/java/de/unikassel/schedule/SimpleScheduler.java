package de.unikassel.schedule;

import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;
import java.util.Set;

public class SimpleScheduler implements Scheduler {

    @Override
    public <T> TaskPrediction<T> schedule(RemoteCallable<T> task, double timePrediction, double[] resourcePrediction,
                                   Set<InetSocketAddress> workers) {
        return new TaskPrediction<>(task, System.currentTimeMillis(), workers.iterator().next(), resourcePrediction);
    }
}
