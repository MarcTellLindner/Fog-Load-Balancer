package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import de.unikassel.util.RemoteCallable;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;

public class LoadBalancer {

    private final LinkedHashSet<InetSocketAddress> workerNodeAddresses;
    private final Kryo kryo;

    public LoadBalancer() {
        workerNodeAddresses = new LinkedHashSet<>();
        kryo = setupKryoInstance();
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

    private <T, K extends Callable<T> & Serializable>
    T executeOnSpecifiedWorker(InetSocketAddress chosenAddress, K callable)
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

         }catch (IOException e) {
            throw new IOException("Exception while executing on worker", e);
        }
    }

    public static Kryo setupKryoInstance() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setDefaultSerializer(new SerializerFactory.FieldSerializerFactory() {
            @Override
            public FieldSerializer<?> newSerializer(Kryo kryo, Class type) {
                FieldSerializer<?> fieldSerializer = new FieldSerializer<>(kryo, type);
                fieldSerializer.getFieldSerializerConfig().setIgnoreSyntheticFields(false);
                return fieldSerializer;
            }
        });
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
//        kryo.register(Object[].class, 123);
//        kryo.register(Class.class, 234);
//        kryo.register(java.lang.invoke.SerializedLambda.class, 345);
        kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer(), 456);
        return kryo;
    }

    public static void main(String... args) throws Exception {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.addWorkerNodeAddresses(new InetSocketAddress(InetAddress.getLocalHost(), WorkerNode.DEFAULT_RPC_PORT));

        try {
            String rs = loadBalancer.executeOnWorker((RemoteCallable<String>) () -> "Test");
            System.out.println(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
