package de.unikassel.schedule.data;

public class ExecutionTimes {
    private long entered;
    private long started;
    private long finished;

    public void entered() {
        this.entered = System.nanoTime();
    }

    public void started() {
        this.started = System.nanoTime();
    }

    public void finished() {
        this.finished = System.nanoTime();
    }

    public long waited() {
        return started - entered;
    }

    public long processed() {
        return finished - started;
    }

    public long retention() {
        return finished - entered;
    }
}
