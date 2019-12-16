package de.unikassel;

import com.googlecode.mobilityrpc.MobilityRPC;
import com.googlecode.mobilityrpc.network.ConnectionId;
import com.googlecode.mobilityrpc.quickstart.EmbeddedMobilityServer;
import com.googlecode.mobilityrpc.session.MobilitySession;
import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.Controller;
import de.unikassel.nativ.jna.ThreadUtil;

import java.io.IOException;
import java.util.concurrent.*;

public class LoadBalancer {

    private final MobilitySession session;
    private final ConnectionId connectionId;

    public LoadBalancer(ConnectionId connectionId) {
        this.connectionId = connectionId;
        this.session = MobilityRPC.newController().newSession();
    }

    public <T> T execute(Callable<T> callable, CGroup cGroup, String sudoPW) {
        return session.execute(connectionId, wrap(callable, cGroup, sudoPW));
    }


    private static <T> Callable<T> wrap(Callable<T> callable, CGroup cGroup, String sudoPW) { // Works only iof static for some reason????
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                Future<T> future = Executors.newSingleThreadExecutor().submit(new Callable<T>() {
                    @Override
                    public T call() throws IOException {
                        cGroup.create(sudoPW);
                        try {
                            cGroup.classify(sudoPW);
                            return callable.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            cGroup.delete(sudoPW);
                        }
                        return null;
                    }
                });
                return future.get();
            }
        };
    }

    public static void main(String... args) throws IOException {

        LoadBalancer worker
                = new LoadBalancer(new ConnectionId("localhost", EmbeddedMobilityServer.DEFAULT_PORT));
        Long tid0 = worker.execute(
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {

                        return ThreadUtil.getThreadId();
                    }
                },
                new CGroup("A", Controller.CPU, Controller.CPUSET),
                System.getenv("password")
        );

        Long tid1 = worker.execute(
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {

                        return ThreadUtil.getThreadId();
                    }
                },
                new CGroup("A", Controller.CPU, Controller.CPUSET),
                System.getenv("password")
        );

        System.out.println(tid0 + " vs " + tid1);

//        CGroup cgA = new CGroup("A", Controller.CPU, Controller.CPUSET)
//                .withOption(CpuSet.CPUS, 0)
//                .withOption(CpuSet.MEMS, 0)
//                .withOption(Cpu.SHARES, 256);
//        CGroup cgB = new CGroup("B", Controller.CPU, Controller.CPUSET)
//                .withOption(CpuSet.CPUS, 0)
//                .withOption(CpuSet.MEMS, 0)
//                .withOption(Cpu.SHARES, 1024);
//
//        String pw = System.getenv("password");
//
//        for (CGroup cg : new CGroup[]{cgA, cgB}) {
//
//            cg.create(pw);
//
//            new Thread(() -> {
//                try {
//                    cg.classify(pw);
//                    for (int i = 0; i < 10; ++i) {
//                        long timeStamp = System.currentTimeMillis();
//                        for (long j = 0; j < Integer.MAX_VALUE; ++j) {
//                            assert true;
//                        }
//                        timeStamp = System.currentTimeMillis() - timeStamp;
//                        System.out.println(cg.name + ": " + timeStamp);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    try {
//                        cg.delete(pw);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//        }
    }
}
