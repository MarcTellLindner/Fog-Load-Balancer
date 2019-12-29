package de.unikassel.cgroup;

/**
 * Controllers usable with cgroup.
 */
public enum Controller {

    BLKIO,
    CPU,
    // CPUACCT,
    CPUSET,
    // DEVICE,
    // FREEZER,
    MEMORY,
    // NET_CLS,
    // NET_PRIO;
    ;

    private final String lowerCaseName = this.name().toLowerCase();

    @Override
    public String toString() {
        return lowerCaseName;
    }
}
