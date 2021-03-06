package de.unikassel.util.serialization;

import de.unikassel.cgroup.CGroup;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * This interface should be used the same way a regular {@link Callable} would be used.
 * <p>
 * The fact that this one implements Serializable is made necessary by {@link com.esotericsoftware.kryo.Kryo}.
 * IMPORTANT: The implementations of this interface don't need to actually be serializable in the Java-way (so it does
 * not matter, whether all fields are serializable etc.).
 *
 * @param <T> Return type.
 */
@FunctionalInterface
public interface RemoteCallable<T> extends Callable<T>, Serializable {

    @Override
    T call() throws Exception;

    default CGroup getCGroup() {
        return null;
    }

    default String sudoPW() {
        return "";
    }

}
