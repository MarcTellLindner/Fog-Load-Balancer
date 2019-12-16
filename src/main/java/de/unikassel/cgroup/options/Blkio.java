package de.unikassel.cgroup.options;

import de.unikassel.cgroup.Controller;

public enum Blkio implements Option {
    RESET_STATS,
    TIME,
    SECTORS,
    AVG_QUEUE_SIZE,
    GROUP_WAIT_TIME,
    EMPTY_TIME,
    IDLE_TIME,
    DEQUEUE,
    IO_SERVICED,
    IO_SERVICE_BYTES,
    IO_SERVICE_TIME,
    IO_WAIT_TIME,
    IO_MERGED,
    IO_IO_QUEUED;

    private final String lowerCaseName = this.name().toLowerCase();


    @Override
    public String toString() {
        return lowerCaseName;
    }

    @Override
    public Controller getController() {
        return Controller.BLKIO;
    }


    public enum Throttle implements Option {
        READ_BPS_DEVICE,
        WRITE_BPS_DEVICE,
        READ_IOPS_DEVICE,
        WRITE_IOPS_DEVICE,
        IO_SERVICED,
        IO_SERVICE_BYTES,
        IO_QUEUED;

        private final String lowerCaseName = String.format("%s.%s", this.getClass().getSimpleName(), this.name())
                .toLowerCase();

        @Override
        public String toString() {
            return lowerCaseName;
        }

        @Override
        public Controller getController() {
            return Controller.BLKIO;
        }
    }


}
