package de.unikassel;

import de.unikassel.cgroup.CGroup;
import de.unikassel.cgroup.Controller;
import de.unikassel.cgroup.options.Cpu;
import de.unikassel.cgroup.options.Memory;
import de.unikassel.schedule.SimpleScheduler;
import de.unikassel.util.serialization.RemoteCallable;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

public class LoadBalancerTest {

    @Test
    public void test() {

        try (LoadBalancer loadBalancer = new LoadBalancer(new SimpleScheduler(), x -> new double[1], x -> new double[1],
                predictions -> new CGroup("Test", Controller.CPU, Controller.MEMORY)
                        .withOption(Cpu.SHARES, 1024)
                        .withOption(Memory.LIMIT_IN_BYTES, 1024))
        ) {
            loadBalancer.addWorkerNodeAddress(
                    new InetSocketAddress(InetAddress.getLocalHost(), WorkerNode.DEFAULT_RPC_PORT),
                    System.getenv("password"));
            String testString = "SUCCESS";
            String anonymous = loadBalancer.executeOnWorker(new RemoteCallable<String>() {
                @Override
                public String call() {
                    return testString;
                }
            }).get();
            String lambda = loadBalancer.executeOnWorker(() -> testString).get();

            assertEquals(testString, anonymous);
            assertEquals(testString, lambda);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
