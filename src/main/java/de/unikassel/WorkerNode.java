package de.unikassel;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

public class WorkerNode implements AutoCloseable {

    public static final int DEFAULT_MONITORING_PORT = 42042;
    public static final int DEFAULT_RPC_PORT = 42043;

    private final ServerSocket serverSocket;
    private final Kryo kryo;

    public WorkerNode(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        kryo = LoadBalancer.setupKryoInstance();
    }

    public void start() throws IOException {
        for (Socket socket = serverSocket.accept(); socket != null; socket = serverSocket.accept()) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            new Thread(() -> {
                try (
                        Input in = new Input(inputStream);
                        Output out = new Output(outputStream)
                ) {
                    Callable<?> callable = (Callable<?>) kryo.readClassAndObject(in);
                    try {
                        Object result = callable.call();
                        kryo.writeClassAndObject(out, result);
                        out.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        kryo.writeClassAndObject(out, null);
                        out.flush();
                    }
                }
            }).start();
        }
    }

    public void stop() throws IOException {
        this.serverSocket.close();
    }

    @Override
    public void close() throws IOException {
        this.stop();
    }

    public static void main(String... args) {
        try (
                WorkerNode workerNode = new WorkerNode(DEFAULT_RPC_PORT)
        ) {
            workerNode.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
