package de.unikassel.cgroup.options;

import de.unikassel.cgroup.Controller;

public enum Cpu implements Option {
    CFS_PERIOD_US,
    CFS_QUOTA_US,
    STAT,
    SHARES,
    RT_PERIOD_US,
    RT_RUNTIME_US;

    private final String lowerCaseName = this.name().toLowerCase();

    @Override
    public String toString() {
        return lowerCaseName;
    }

    @Override
    public Controller getController() {
        return Controller.CPU;
    }
}
