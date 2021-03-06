package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.unikassel.cgroup.CGroup;
import de.unikassel.util.security.RemoteCallableRestrictingSecurityManager;
import de.unikassel.util.serialization.RemoteCallable;
import de.unikassel.util.serialization.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PermissionCollection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A node to execute tasks provided by a {@link LoadBalancer} over the network.
 */
public class WorkerNode implements AutoCloseable {

    public static final int DEFAULT_MONITORING_PORT = 42042;
    public static final int DEFAULT_RPC_PORT = 42043;

    private final ServerSocket serverSocket;
    private final ExecutorService executorService;

    /**
     * Create a new node to accept tasks on the default port ({@link WorkerNode#DEFAULT_RPC_PORT}).
     *
     * @throws IOException In case the port is already in use.
     */
    public WorkerNode() throws IOException {
        this(DEFAULT_RPC_PORT);
    }

    /**
     * Create a new node to accept tasks on the specified port.
     *
     * @param port The port to bind to.
     * @throws IOException In case the port is already in use.
     */
    public WorkerNode(int port) throws IOException {
        this(port, 64);
    }

    /**
     * Create a new node to accept tasks on the specified port with a limited amount of threads.
     *
     * @param port           The port to bind to.
     * @param maxThreadCount The maximal number of threads to run simultaneously.
     * @throws IOException In case the port is already in use.
     */
    public WorkerNode(int port, int maxThreadCount) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.executorService = Executors.newWorkStealingPool(maxThreadCount);
    }

    /**
     * Start listening for tasks to execute.
     *
     * @param permissions Permission required for task execution.
     * @throws IOException In case of problems with the connection.
     */
    public void start(PermissionCollection permissions) throws IOException {
        RemoteCallableRestrictingSecurityManager.install(permissions);
        for (Socket socket = serverSocket.accept(); socket != null; socket = serverSocket.accept()) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            this.executorService.submit(() -> {
                try (
                        Input in = new Input(inputStream);
                        Output out = new Output(outputStream)
                ) {
                    Kryo kryo = Serializer.setupKryoInstance();
                    RemoteCallable<?> callable = (RemoteCallable<?>) kryo.readClassAndObject(in);
                    try {
                        Object result;
                        if (callable.getCGroup() != null) {
                            CGroup cGroup = callable.getCGroup();
                            String sudoPW = callable.sudoPW();

                            cGroup.create(sudoPW);
                            cGroup.classify(sudoPW);

                            result = callable.call();

                            cGroup.delete(sudoPW);

                        } else {
                            result = callable.call();
                        }
                        kryo.writeClassAndObject(out, result);
                        out.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        kryo.writeClassAndObject(out, null);
                        out.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Stop listening for tasks.
     *
     * @throws IOException In case of problems while closing the socket.
     */
    public void stop() throws IOException {
        this.serverSocket.close();
        RemoteCallableRestrictingSecurityManager.uninstall();
    }

    @Override
    public void close() throws IOException {
        this.stop();
    }
}
