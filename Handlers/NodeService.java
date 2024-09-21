package Handlers;

import Client.Node;

/**
 * This class is responsible for the node comunication on the network
 */
public class NodeService {

    private String ip;
    private int port;

    public NodeService(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    // TODO: Implement a router/Driver class to handle the node operations (from the interface to the methods)
}
