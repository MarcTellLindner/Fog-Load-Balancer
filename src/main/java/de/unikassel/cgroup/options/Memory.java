package de.unikassel.cgroup.options;

import de.unikassel.cgroup.Controller;

public enum Memory implements Option {
    STAT,
    USAGE_IN_BYTES,
    MAX_USAGE_IN_BYTES,
    LIMIT_IN_BYTES,
    FAILCNT,
    FORCE_EMPTY,
    SWAPPINESS,
    USE_HIERARCHY,
    OOM_CONTROLL;

    private final String lowerCaseName = this.name().toLowerCase();

    @Override
    public String toString() {
        return lowerCaseName;
    }

    @Override
    public Controller getController() {
        return Controller.MEMORY;
    }

    public enum Memsw implements Option {
        STAT,
        USAGE_IN_BYTES,
        MAX_USAGE_IN_BYTES,
        LIMIT_IN_BYTES,
        FAILCNT;

        private final String lowerCaseName = String.format("%s.%s", this.getClass().getSimpleName(), this.name())
                .toLowerCase();

        @Override
        public String toString() {
            return lowerCaseName;
        }

        @Override
        public Controller getController() {
            return Controller.MEMORY;
        }
    }
}
