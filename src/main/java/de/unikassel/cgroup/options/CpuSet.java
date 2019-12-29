package de.unikassel.cgroup.options;

import de.unikassel.cgroup.Controller;

/**
 * Options of the cpuset-{@link Controller}.
 */
public enum CpuSet implements Option {
    CPUS,
    MEMS,
    MEMORY_MIGRATE,
    CPU_EXCLUSIVE,
    MEM_EXCLUSIVE,
    MEM_HARDWALL,
    MEMORY_PRESSURE,
    MEMORY_PRESSURE_ENABLED,
    MEMORY_SPREAD_PAGE,
    MEMORY_SPREAD_SLAB,
    SCHED_LOAD_BALANCE,
    SCHED_RELAX_DOMAIN_LEVEL;

    private final String lowerCaseName = this.name().toLowerCase();

    @Override
    public String toString() {
        return lowerCaseName;
    }

    @Override
    public Controller getController() {
        return Controller.CPUSET;
    }
}
