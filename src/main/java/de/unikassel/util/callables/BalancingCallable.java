package de.unikassel.util.callables;

import de.unikassel.cgroup.CGroup;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class BalancingCallable<T> implements Callable<T> {
    private final Callable<T> callable;
    private final CGroup cGroup;
    private final String sudoPW;

    public BalancingCallable( Callable<T> callable, CGroup cGroup, String sudoPW) {
        this.callable = callable;
        this.cGroup = cGroup;
        this.sudoPW = sudoPW;
    }

    @Override
    public T call() throws Exception {
        cGroup.create(this.sudoPW);
        Future<T> future = newSingleThreadExecutor().submit(new ClassifiedCallable());
        return future.get();
    }

    private class ClassifiedCallable implements Callable<T> {
        @Override
        public T call() throws Exception {
            cGroup.classify(sudoPW);
            return callable.call();
        }
    }
}
