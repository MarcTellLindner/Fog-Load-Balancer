package de.unikassel;

import com.googlecode.mobilityrpc.controller.MobilityController;
import com.googlecode.mobilityrpc.quickstart.EmbeddedMobilityServer;

public class WorkerNode {
    public static void main(String... args) {
        MobilityController server = EmbeddedMobilityServer.start();
    }
}
