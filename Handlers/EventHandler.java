package handlers;

import java.math.BigInteger;

import Events.*;
import handlers.*;
import Message.*;
import client.Node;
import client.UserService;
import dtos.*;

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
        NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);
        boolean isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);
        NodeDTO nodeToEnterDTO = event.getToEnter();
        UserDTO currentUserDTO = userService.getCurrentUser();
        NodeDTO currentNodeDTO = userService.getCurrentNodeDTO();
        Node currentNode = userService.getCurrentNode();

        if (nodeWithHash == null  && isDefaultNode) { // network is empty
            // Update the default node
            userService.getCurrentNode().setNextNode(event.getToEnter());
            userService.getCurrentNode().setPreviousNode(event.getToEnter());

            // Update the new node
            ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateNeighbors, userService.getCurrentUser(), null, currentNodeDTO, currentNodeDTO);
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), message);

            // TODO: Update the finger tables
        } else if (nodeWithHash == null) { // target node is the current node
            String ipNext = userService.getCurrentNode().getNextNode().getIp();
            int portNext = userService.getCurrentNode().getNextNode().getPort();
            String ipPrev = userService.getCurrentNode().getPreviousNode().getIp();
            int portPrev = userService.getCurrentNode().getPreviousNode().getPort();

            userService.startClient(ipNext, portNext, new ChordInternalMessage(MessageType.UpdateNeighbors, currentUserDTO, null, null, event.getToEnter())); // mudar prev do next para o novo node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, currentUserDTO, null, currentNode.getNextNode(), null)); // mudar next do novo node para o next do current
            // mudar next do current para o novo node
            userService.startClient(, , new Message()); // mudar prev do novo node para o current
            // TODO: Update the finger tables
        } else { // foward to the next node
            userService.startClient(nodeWithHash.getIp(), nodeWithHash.getPort(), event.getMessage());
        }
    }

    private void enterNodeAux () {

    }
}
