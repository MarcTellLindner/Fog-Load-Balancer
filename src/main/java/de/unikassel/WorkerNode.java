package de.unikassel;

import com.googlecode.mobilityrpc.MobilityRPC;
import com.googlecode.mobilityrpc.controller.MobilityController;
import com.googlecode.mobilityrpc.network.ConnectionId;
import com.googlecode.mobilityrpc.quickstart.EmbeddedMobilityServer;
import com.googlecode.mobilityrpc.quickstart.util.NetworkUtil;

import java.util.List;

public class WorkerNode {

    public static final int DEFAULT_MONITORING_PORT = 42042;
    public static final int DEFAULT_RPC_PORT = 42043;

    private final MobilityController mobilityController;
    private final int port;

    public WorkerNode(int port) {
        this.port = port;
        mobilityController = MobilityRPC.newController();
    }

    public void start() {
        List<String> bindAddresses = NetworkUtil.getAllNetworkInterfaceAddresses();
        for (String networkAddress : bindAddresses) {
            mobilityController.getConnectionManager().bindConnectionListener(
                    new ConnectionId(networkAddress, port));
        }
    }

    public void stop() {
        mobilityController.destroy();
    }

    public static void main(String... args) {
        WorkerNode workerNode = new WorkerNode(DEFAULT_RPC_PORT);
        workerNode.start();
    }
}
