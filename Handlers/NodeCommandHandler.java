package handlers;

import java.io.IOException;

public class NodeCommandHandler {

    private NodeThread currentThread;

    public NodeCommandHandler(NodeThread nodeThread) {
        this.currentThread = nodeThread;
    }

    public void processCommand(String command) {
        if (command.equals("End Connection")) {
            endConectionRequest();
        }
    }

    public void endConectionRequest() {
        try {
            currentThread.getSocket().close();
            System.out.println("Connection closed.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            currentThread.interrupt(); // Interrupt the thread to terminate it
        }
    }
}
