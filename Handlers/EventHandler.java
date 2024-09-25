package handlers;

import java.math.BigInteger;

import Events.*;
import client.UserService;
import dtos.NodeDTO;

public class EventHandler {

    private UserService userService;

    public EventHandler(UserService userService) {
        this.userService = userService;
    }

    public void enterNode(EnterNodeEvent event) {
        BigInteger hash = event.getToEnter().getHash();
        NodeDTO nodeWithHash = userService.getNodeWithHash(hash);

        if (nodeWithHash == null) {
            userService.setNextNode(nodeWithHash);
            // TODO: Update the finger table of the current node
            // TODO: Send a message to next node to update its finger table and next node
        } else {
            // Continue to the next node
        }
    }
}
