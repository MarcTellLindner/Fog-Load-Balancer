package de.unikassel;

import com.googlecode.mobilityrpc.MobilityRPC;
import com.googlecode.mobilityrpc.controller.MobilityController;
import com.googlecode.mobilityrpc.network.ConnectionId;
import com.googlecode.mobilityrpc.session.MobilitySession;
import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.Controller;
import de.unikassel.nativ.jna.ThreadUtil;
import de.unikassel.util.callables.BalancingCallable;
import de.unikassel.util.callables.DestroyRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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
        return sessions.get(connectionId).execute(connectionId, new BalancingCallable<T>(callable, cGroup, sudoPW));
    }

    public void destroy() {
        for (Map.Entry<ConnectionId, MobilitySession> entry : sessions.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            MobilitySession mobilitySession = entry.getValue();

            mobilitySession.execute(connectionId, new DestroyRunnable());
        }
        sessions.clear();
        controller.destroy();
    }

    public static void main(String... args) throws IOException {

        LoadBalancer balancer
                = new LoadBalancer();

        Long tid0 = balancer.execute(
                new ConnectionId("localhost", WorkerNode.DEFAULT_RPC_PORT),
                new Callable<Long>() {
                    @Override
                    public Long call() {

                        return ThreadUtil.getThreadId();
                    }
                },
                new CGroup("A", Controller.CPU, Controller.CPUSET),
                System.getenv("password")
        );
        System.out.print(tid0 + " vs ");

                Long tid1 = balancer.execute(
                new ConnectionId("localhost", WorkerNode.DEFAULT_RPC_PORT),
                new Callable<Long>() {
                    @Override
                    public Long call() {

                        return ThreadUtil.getThreadId();
                    }
                },
                new CGroup("B", Controller.CPU, Controller.CPUSET),
                System.getenv("password")
        );

        System.out.println(tid1);

        balancer.destroy();
    }
}
