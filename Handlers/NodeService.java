package Handlers;

import java.util.ArrayList;

import Client.Node;

/**
 * This class is responsible for handling the node operations on a network base
 * Singleton pattern is used to ensure that only one instance of the class is created
 */
public class NodeService {

    // Private static instance of the class
    private static NodeService instance;

    // Private constructor to prevent instantiation
    private NodeService() {
    }

    // Public method to provide access to the instance
    public static synchronized NodeService getInstance() {
        if (instance == null) {
            instance = new NodeService();
        }
        return instance;
    }

    /**
     * Enters a node into the network
     * 
     * @param node
     */
    private void enterNode(Node node, Node prevNode) {
        if (prevNode == null)
            return;

        // Update the finger table of the previous node

    }

    /**
     * Obtains the updated finger table of the node
     * 
     * @param counter 
     * @param node
     * @param fingerTable
     * @return
     */
    private ArrayList<Node> getUpdateFingerTable(int counter, Node currentNode, Node node, ArrayList<Node> fingerTable) {
        if (currentNode.equals(node))
            return fingerTable;
        // TODO: Implement the logic to update the finger table
    }
}
