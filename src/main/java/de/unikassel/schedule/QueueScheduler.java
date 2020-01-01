package de.unikassel.schedule;

import de.unikassel.schedule.data.TaskPrediction;
import de.unikassel.schedule.data.WorkerResources;
import de.unikassel.util.serialization.RemoteCallable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueueScheduler implements Scheduler {


    private final LinkedHashSet<TaskPrediction<?>> waiting = new LinkedHashSet<>();
    private final HashSet<TaskPrediction<?>> processed = new HashSet<>();


    @Override
    public <T> TaskPrediction<T> schedule(RemoteCallable<T> task, double timePrediction, double[] resourcePrediction,
                                   Set<InetSocketAddress> workers) {
        List<TaskPrediction<?>> allTasks = new ArrayList<>();
        allTasks.addAll(this.waiting);
        allTasks.addAll(this.processed);
        allTasks.sort(Comparator.comparingDouble(a -> a.time));

        ArrayList<WorkerResources> allResources = new ArrayList<>();
        LinkedHashMap<InetSocketAddress, ArrayList<WorkerResources>> workerResources
                = new LinkedHashMap<>(workers.stream().collect(Collectors.toMap(w -> w, w -> new ArrayList<>())));

        for (InetSocketAddress worker : workers) {
            WorkerResources res = null; // TODO: measure
            allResources.add(res);
            workerResources.get(worker).add(res);
        }

        for (TaskPrediction<?> taskPrediction : allTasks) {
            // Access the previously inserted value
            WorkerResources measured = workerResources.get(taskPrediction.worker).get(0);

            double[] simulatedResources = IntStream.range(0, measured.freeResources.length)
                    .mapToDouble(i -> measured.freeResources[i] + taskPrediction.resources[i]).toArray();

            WorkerResources simulated = new WorkerResources(taskPrediction.time /* TODO: check */,
                    measured.workerAddress,
                    simulatedResources);
        }

        for (WorkerResources current : allResources) {
            WorkerResources next = current;
            do {
                double[] freeResources = next.freeResources;
                if(IntStream.range(0, freeResources.length)
                        .allMatch(i -> resourcePrediction[i] <= freeResources[i])) {
                    ArrayList<WorkerResources> worker = workerResources.get(current.workerAddress);
                    int nextIndex = worker.indexOf(current) + 1;
                    if(nextIndex >= worker.size()) {
                        break; // TODO: Break what?
                    }
                    next = worker.get(nextIndex);
                } else {
                    continue;
                }

                double[] negativeResourcePrediction = Arrays.stream(resourcePrediction).map(d -> -d).toArray();
                allTasks.add(new TaskPrediction<>(task, current.timestamp,
                        current.workerAddress, negativeResourcePrediction));
                allTasks.add(new TaskPrediction<>(task,
                        current.timestamp + timePrediction -System.currentTimeMillis(),
                        current.workerAddress, resourcePrediction));
                return null; // TODO
            } while (!(timePrediction > (next.timestamp - current.timestamp)));
        }
        return null; // TODO
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
}
