package handlers;

import java.math.BigInteger;
import java.util.ArrayList;

import Events.*;
import Message.*;
import client.Node;
import client.UserService;
import dtos.*;

public class EventHandler { //TODO: Will there not be problems with the threads? Like the updates havent propagated yet?

    private UserService userService;

    String ipDefault;
    int portDefault;

    public EventHandler(UserService userService) {
        this.userService = userService;
        ipDefault = userService.getIpDefault();
        portDefault = userService.getPortDefault();
    }

    public synchronized void enterNode(EnterNodeEvent event) {
        BigInteger hash = event.getToEnterHash();
        NodeDTO nodeToEnterDTO = event.getToEnter();

        NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);
        boolean isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);
        NodeDTO currentNodeDTO = userService.getCurrentNodeDTO();
        Node currentNode = userService.getCurrentNode();

        if (nodeWithHashDTO == null  && isDefaultNode) { // network is empty and the current node is the default node
            // Update the default node (next and prev are now the new node)
            userService.getCurrentNode().setNextNode(event.getToEnter());
            userService.getCurrentNode().setPreviousNode(event.getToEnter());

            // Update the new node
            ChordInternalMessage messageN = new ChordInternalMessage(MessageType.UpdateNeighbors, nodeToEnterDTO, hash, currentNodeDTO, currentNodeDTO); // prev and next are the current node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), messageN);

            // Update the finger tables
            ChordInternalMessage messageFT = new ChordInternalMessage(MessageType.UpdateFingerTable, null, currentNodeDTO, 0);
            userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), messageFT);

        } else if (nodeWithHashDTO == null) { // target node (Prev to the new Node) is the current node
            NodeDTO nextNodeDTO = currentNode.getNextNode();
            String ipNext = nextNodeDTO.getIp();
            int portNext = nextNodeDTO.getPort();

            // Update the neighbors
            userService.startClient(ipNext, portNext, new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), nextNodeDTO.getHash(), null, event.getToEnter())); // mudar prev do next para o novo node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, nodeToEnterDTO, nodeToEnterDTO.getHash(), currentNode.getNextNode(), null)); // mudar next do novo node para o next do current
            currentNode.setNextNode(nodeToEnterDTO);// mudar next do current para o novo node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, nodeToEnterDTO, nodeToEnterDTO.getHash(), null, currentNodeDTO)); // mudar prev do novo node para o current

            // Update the finger tables
            ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateFingerTable, null, userService.getCurrentNodeDTO(), 0);
            userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), message);
        } else { // foward to the next node
            userService.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage());
        }
    }

    public void exitNode() { // What about the threads?
        NodeDTO prevNodeDTO = userService.getCurrentNode().getPreviousNode();
        NodeDTO nextNodeDTO = userService.getCurrentNode().getNextNode();
        Node currentNode = userService.getCurrentNode();

        if (prevNodeDTO == null && nextNodeDTO == null) // the current node is the only node in the network (default node) 
            return;
        
        // mudar next do prev para o next do current
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getPreviousNode(), currentNode.getPreviousNode().getHash(), currentNode.getNextNode(), null);
        userService.startClient(currentNode.getPreviousNode().getIp(), currentNode.getPreviousNode().getPort(), message);
        
        // mudar prev do next para o prev do current
        ChordInternalMessage message2 = new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), currentNode.getNextNode().getHash(), null, currentNode.getPreviousNode());
        userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), message2);

        // Update the finger tables
        ChordInternalMessage message3 = new ChordInternalMessage(MessageType.UpdateFingerTable, null, userService.getCurrentNodeDTO(), 0);
        userService.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), message3);
    }

    public synchronized void updateNeighbors(UpdateNeighboringNodesEvent event) {
        BigInteger hash = event.getMessage().getReciverHash();
        NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);
        boolean isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);

        if (nodeWithHashDTO == null  && isDefaultNode) { // network is empty and the current node is the default node
            return;
        } else if (nodeWithHashDTO == null) { // target node 
            NodeDTO nextNode = event.getNext();
            NodeDTO previousNode = event.getPrevious();

            if (nextNode != null)
                userService.getCurrentNode().setNextNode(nextNode);
            if (previousNode != null)
                userService.getCurrentNode().setPreviousNode(previousNode);
        } else { // foward to the next node
            userService.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage());
        }
    }

    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO currNodeDTO = userService.getCurrentNodeDTO();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate();
        ArrayList<NodeDTO> fingerTable = userService.getCurrentNode().getFingerTable();
        int ringSize = userService.getRingSize();

        if (currNodeDTO.equals(nodeToUpdateDTO)) {
            userService.getCurrentNode().setFingerTable(fingerTable);
            return;
        } else if (counter == userService.getHashLength()) {
            userService.startClient(nodeToUpdateDTO.getIp(), nodeToUpdateDTO.getPort(), message);
            return;
        }

        int distance = (nodeToUpdateDTO.getHash().intValue() - currNodeDTO.getHash().intValue()+ ringSize) % ringSize;
        if (distance >= Math.pow(2, counter)) {
            message.addNodeToFingerTable(currNodeDTO);

            // Skip the next counters if 2^counter is lower than the distance between this and the next node to try
            while (Math.pow(2, counter) <= distance) 
                counter++;
                message.incCounter();
        }

        NodeDTO nextNode = userService.getCurrentNode().getNextNode();
        userService.startClient(nextNode.getIp(), nextNode.getPort(), message);
    }
    
    public void broadcastMessage(BroadcastUpdateFingerTableEvent event) {
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateFingerTable, null, userService.getCurrentNodeDTO(), 0);
        updateFingerTable(new UpdateNodeFingerTableEvent(message));
        if (!event.getInitializer().equals(userService.getCurrentNodeDTO())) // foward to the next node
            userService.startClient(ipDefault, portDefault, event.getMessage());
    }
}
