package de.unikassel.schedule;

import de.unikassel.LoadBalancer;
import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;
import java.util.Set;

public interface Scheduler {
    <T> TaskPrediction<T> schedule(RemoteCallable<T> task, double timePrediction, double[] resourcePrediction,
                            Set<InetSocketAddress> workers);

    default void started(TaskPrediction<?> taskPrediction) {
    }

    default void finished(TaskPrediction<?> taskPrediction) {
    }
}
