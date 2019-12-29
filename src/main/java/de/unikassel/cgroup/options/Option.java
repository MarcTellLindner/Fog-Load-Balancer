package de.unikassel.cgroup.options;

import de.unikassel.cgroup.Controller;

/**
 * Options provided by the different {@link Controller}-constants.
 */
public interface Option {
    /**
     * Get the {@link Controller} of this Option.
     *
     * @return The {@link Controller}.
     */
    Controller getController();
}
