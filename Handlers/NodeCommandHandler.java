package handlers;

import java.io.IOException;
import java.math.BigInteger;

public class NodeCommandHandler {

    private NodeThread currentThread;

    public NodeCommandHandler(NodeThread nodeThread) {
        this.currentThread = nodeThread;
    }

    public void processCommand(String command) {
        if (command.equals("End Connection")) {
            endConectionRequest();
        } else if (command.contains("Enter Node:")) {
            enterNode(command);
        }
    }

    private void endConectionRequest() {
        try {
            currentThread.getSocket().close();
            System.out.println("Connection closed.");
            System.exit(-1); // TODO: Check if this is the best way to close the thread
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enterNode(String command) {
        String[] splitedCmd = command.split("Enter Node:");
        if (splitedCmd.length > 1) {
            String bigIntegerStr = splitedCmd[1].trim();
            try {
                BigInteger nodeHash = new BigInteger(bigIntegerStr);
                // TODO: Continue the logic

            } catch (NumberFormatException e) {
                System.err.println("Invalid BigInteger format: " + bigIntegerStr);
            }
        }
    }
}
