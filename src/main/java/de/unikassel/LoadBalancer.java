package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.unikassel.util.serialization.RemoteCallable;
import de.unikassel.util.serialization.Serializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

public class LoadBalancer {

    private final LinkedHashSet<InetSocketAddress> workerNodeAddresses;
    private final Kryo kryo;

    public LoadBalancer() {
        workerNodeAddresses = new LinkedHashSet<>();
        kryo = Serializer.setupKryoInstance();
    }

    public void addWorkerNodeAddresses(InetSocketAddress... addresses) {
        this.addWorkerNodeAddresses(Arrays.asList(addresses));
    }

    public void addWorkerNodeAddresses(Collection<InetSocketAddress> addresses) {
        this.workerNodeAddresses.addAll(addresses);
    }

    public <T> T executeOnWorker(RemoteCallable<T> callable) throws IOException {
        InetSocketAddress chosenAddress = workerNodeAddresses.iterator().next(); // Placeholder for actual algorithm
        return executeOnSpecifiedWorker(chosenAddress, callable);
    }

    private <T> T executeOnSpecifiedWorker(InetSocketAddress chosenAddress, RemoteCallable<T> callable)
            throws IOException {
        try (
                Socket worker = new Socket(chosenAddress.getAddress(), chosenAddress.getPort());
                Output out = new Output(worker.getOutputStream());
                Input in = new Input(worker.getInputStream())
        ) {
            kryo.writeClassAndObject(out, callable);
            out.flush();

            @SuppressWarnings("unchecked")
            T result = (T) kryo.readClassAndObject(in);
            return result;

        } catch (IOException e) {
            throw new IOException("Exception while executing on worker", e);
        }
    }

    public static void main(String... args) throws Exception {

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.addWorkerNodeAddresses(new InetSocketAddress(InetAddress.getLocalHost(), WorkerNode.DEFAULT_RPC_PORT));

        try {
            String testString = "SUCCESS";
            String anonymous = loadBalancer.executeOnWorker(new RemoteCallable<String>() {
                @Override
                public String call() {
                    return testString;
                }
            });
            String lambda = loadBalancer.executeOnWorker(() -> testString);

            System.out.printf("Anonymous:\t%s \nLambda:\t\t%s", anonymous, lambda);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
