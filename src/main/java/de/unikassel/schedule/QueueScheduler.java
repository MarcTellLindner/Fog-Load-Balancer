package de.unikassel.schedule;

import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.schedule.data.WorkerResources;
import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueueScheduler implements Scheduler {

    private final Function<InetSocketAddress, WorkerResources> getCurrentFreeResources;

    private final LinkedHashSet<TaskPrediction<?>> waiting = new LinkedHashSet<>();
    private final LinkedHashSet<TaskPrediction<?>> processed = new LinkedHashSet<>();

    /**
     * Create a new queue-based scheduler.
     *
     * @param getCurrentFreeResources Function to request resources of a worker-node.
     */
    public QueueScheduler(Function<InetSocketAddress, WorkerResources> getCurrentFreeResources) {
        this.getCurrentFreeResources = getCurrentFreeResources;
    }


    @Override
    public <T> TaskPrediction<T> schedule(RemoteCallable<T> task, double timePrediction, double[] resourcePrediction,
                                          Set<InetSocketAddress> workers) {

        // A change happens, whenever a task starts or ends
        TreeMap<Double, Map.Entry<InetSocketAddress, double[]>> allChanges = new TreeMap<>(Double::compare);
        for (TaskPrediction<?> taskWaiting : this.waiting) {
            allChanges.putAll(predictions(taskWaiting, false)); // Waiting will start and end
        }
        for (TaskPrediction<?> processedTask : this.processed) {
            allChanges.putAll(predictions(processedTask, true)); // Processed tasks already started and will end
        }

        // Create a map, accessed by the worker, that holds all changes of this worker with accumulated values
        Map<InetSocketAddress, TreeMap<Double, double[]>> totalWorkerResources = workers.stream().collect(Collectors
                .toMap(
                        w -> w,
                        w -> new TreeMap<>(Double::compareTo)
                ));

        // Get the currently free resources per worker
        for (InetSocketAddress worker : workers) {
            WorkerResources res = getCurrentFreeResources.apply(worker);
            totalWorkerResources.get(worker).put(res.timestamp, res.freeResources);
        }

        // Add all scheduled changes to the worker, on which each task will be executed
        for (Map.Entry<Double, Map.Entry<InetSocketAddress, double[]>> change : allChanges.entrySet()) {

            // Get values of the change
            Double time = change.getKey();
            InetSocketAddress worker = change.getValue().getKey();
            double[] res = change.getValue().getValue();

            // Access the previously inserted value
            double[] previousResources = totalWorkerResources.get(worker).lastEntry().getValue();

            // Apply the change
            double[] simulatedResources = add(previousResources, res);

            // Remember the value
            totalWorkerResources.get(worker).put(time, simulatedResources);
        }

        // Flatten all collected information in a list of WorkerResources
        WorkerResources[] resources = totalWorkerResources.entrySet().stream()
                .flatMap(eOuter -> eOuter.getValue().entrySet().stream()
                        .map(eInner -> new WorkerResources(eInner.getKey(), eOuter.getKey(), eInner.getValue()))
                ).toArray(WorkerResources[]::new);

        // Sort by timestamp
        Arrays.sort(resources, Comparator.comparingDouble(a -> a.timestamp));

        for (int i = 0; i < resources.length; ++i) {
            // A time and worker, this task might be scheduled at
            WorkerResources current = resources[i];
            for (int j = i; j < resources.length; ++j) {
                // All changes following current, starting with current itself
                WorkerResources next = resources[j];

                if (next.workerAddress.equals(current.workerAddress) // Is this our worker?
                        && !lessEqual(resourcePrediction, next.freeResources)) {
                    break; // Some other task will take the required resources -> no match
                }
                if (timePrediction > next.timestamp - current.timestamp && j + 1 < resources.length) {
                    continue; // Resources are still available, but this might change later -> proceed to check
                }

                // We found a match -> schedule task here!
                TaskPrediction<T> scheduled = new TaskPrediction<>(task, current.timestamp, timePrediction,
                        current.workerAddress, resourcePrediction);

                waiting.add(scheduled); // Remember
                return scheduled;
            }
        }
        // No match was found. This may only occur, if no worker has enough resources, even if this is the only task
        // -> the task was not scheduled
        return null;
    }

    @Override
    public void started(TaskPrediction<?> taskPrediction) {
        this.waiting.remove(taskPrediction);
        this.processed.add(taskPrediction);
    }

    @Override
    public void finished(TaskPrediction<?> taskPrediction) {
        this.processed.remove(taskPrediction);
    }

    private Map<Double, Map.Entry<InetSocketAddress, double[]>> predictions(TaskPrediction<?> task, boolean started) {
        HashMap<Double, Map.Entry<InetSocketAddress, double[]>> map = new HashMap<>();
        // If task started already, skip this step
        if (!started) {
            // When the task starts, it takes the required resources
            map.put(Math.max(task.time, System.currentTimeMillis()),
                    new AbstractMap.SimpleImmutableEntry<>(task.worker, negative(task.resources)));
        }
        // When it finishes, the resources become available again
        map.put(Math.max(task.time + task.duration, System.currentTimeMillis()),
                new AbstractMap.SimpleImmutableEntry<>(task.worker, task.resources));
        return map;
    }

    private double[] negative(double[] positive) {
        return Arrays.stream(positive).map(d -> -d).toArray();
    }

    private double[] add(double[] a, double[] b) {
        return IntStream.range(0, a.length).mapToDouble(i -> a[i] + b[i]).toArray();
    }

    private boolean lessEqual(double[] a, double[] b) {
        return IntStream.range(0, a.length).allMatch(i -> a[i] <= b[i]);
    }
}
