package de.unikassel;

import com.googlecode.mobilityrpc.MobilityRPC;
import com.googlecode.mobilityrpc.controller.MobilityController;
import com.googlecode.mobilityrpc.network.ConnectionId;
import com.googlecode.mobilityrpc.session.MobilityContext;
import com.googlecode.mobilityrpc.session.MobilitySession;
import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.Controller;
import de.unikassel.nativ.jna.ThreadUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoadBalancer {

    private final MobilityController controller;
    private final HashMap<ConnectionId, MobilitySession> sessions;

    public LoadBalancer() {
        controller = MobilityRPC.newController();
        sessions = new HashMap<>();
    }

    public <T> T execute(ConnectionId connectionId, Callable<T> callable, CGroup cGroup, String sudoPW) {
        if (!sessions.containsKey(connectionId)) {
            sessions.put(connectionId, controller.newSession());
        }
        return sessions.get(connectionId).execute(connectionId, wrap(callable, cGroup, sudoPW));
    }

    public void destroy() {
        for (Map.Entry<ConnectionId, MobilitySession> entry : sessions.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            MobilitySession mobilitySession = entry.getValue();

            mobilitySession.execute(connectionId, new Runnable() {
                @Override
                public void run() {
                    MobilityContext.getCurrentSession().release();
                }
            });
        }
        sessions.clear();
        controller.destroy();
    }

    private static <T> Callable<T> wrap(Callable<T> callable, CGroup cGroup, String sudoPW) { // Works only if static for some reason????
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

        LoadBalancer balancer
                = new LoadBalancer();
        Long tid0 = balancer.execute(
                new ConnectionId("localhost", WorkerNode.DEFAULT_RPC_PORT),
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {

                        return ThreadUtil.getThreadId();
                    }
                },
                new CGroup("A", Controller.CPU, Controller.CPUSET),
                System.getenv("password")
        );

        Long tid1 = balancer.execute(
                new ConnectionId("localhost", WorkerNode.DEFAULT_RPC_PORT),
                new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {

                        return ThreadUtil.getThreadId();
                    }
                },
                new CGroup("B", Controller.CPU, Controller.CPUSET),
                System.getenv("password")
        );

        System.out.println(tid0 + " vs " + tid1);

        balancer.destroy();
    }
}
