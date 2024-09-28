package handlers;

import java.math.BigInteger;

import Events.*;
import Message.ChordInternalMessage;
import Message.Message;
import client.UserService;
import dtos.NodeDTO;

public class EventHandler {

    private UserService userService;

    String ipDefault;
    int portDefault;

    public EventHandler(UserService userService) {
        this.userService = userService;
        ipDefault = userService.getIpDefault();
        portDefault = userService.getPortDefault();
    }

    public void enterNode(EnterNodeEvent event) {
        BigInteger hash = event.getToEnter().getHash();
        NodeDTO nodeWithHash = userService.getNodeWithHash(hash);
        boolean isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);

        if (nodeWithHash == null  && isDefaultNode) { // network is empty
            userService.getCurrentNode().setNextNode(event.getToEnter());
            userService.getCurrentNode().setPreviousNode(event.getToEnter());
            // TODO: Update the finger tables
        } else if (nodeWithHash == null) { // target node is the current node
            String ipNext = userService.getCurrentNode().getNextNode().getIp();
            int portNext = userService.getCurrentNode().getNextNode().getPort();
            String ipPrev = userService.getCurrentNode().getPreviousNode().getIp();
            int portPrev = userService.getCurrentNode().getPreviousNode().getPort();

            userService.startClient(ipNext, portNext, new ChordInternalMessage()); // mudar prev do next para o novo node
            userService.startClient(, , new Message()); // mudar next do novo node para o next do current
            // mudar next do current para o novo node
            userService.startClient(, , new Message()); // mudar prev do novo node para o current
            // TODO: Update the finger tables
        } else { // foward to the next node
            userService.startClient(nodeWithHash.getIp(), nodeWithHash.getPort(), event.getMessage());
        }
    }
}
