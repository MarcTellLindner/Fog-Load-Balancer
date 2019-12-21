package de.unikassel.util;

import java.io.Serializable;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface RemoteCallable<T> extends Callable<T>, Serializable {

    @Override
    T call() throws Exception;
}
