package Handlers;

import java.math.BigInteger;
import java.util.ArrayList;

import Events.*;
import Message.*;
import Client.Node;
import Client.UserService;
import dtos.*;

public class EventHandler { //TODO: Will there not be problems with the threads? Like the updates havent propagated yet?

    private UserService userService;

    String ipDefault;
    int portDefault;
    boolean isDefaultNode;
    NodeDTO currentNodeDTO;
    Node currentNode;

    public EventHandler(UserService userService) {
        this.userService = userService;
        ipDefault = userService.getIpDefault();
        portDefault = userService.getPortDefault();
        isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);
        currentNodeDTO = userService.getCurrentNodeDTO();
        currentNode = userService.getCurrentNode();

        System.out.println("Starting event handler...");
        System.out.println("nodeDTO: " + currentNodeDTO.toString());
    }

    public synchronized void updateNeighbors(UpdateNeighboringNodesEvent event) {
        if (event.getNext() != null)
            currentNode.setNextNode(event.getNext());
        if (event.getPrevious() != null)
            currentNode.setPreviousNode(event.getPrevious());
    }

    public synchronized void enterNode(EnterNodeEvent event) { // TODO: Check the nodeWithHashDTO part
        // TODO: If default node is the actual target it does not work??????
        
        BigInteger hash = event.getToEnterHash();
        NodeDTO nodeToEnterDTO = event.getToEnter();
        NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);

        if (nodeWithHashDTO == null  && isDefaultNode && (currentNode.getFingerTable().size()==0)) { // network is empty and the current node is the default node
            // Update the default node (next and prev are now the new node)
            userService.getCurrentNode().setNextNode(event.getToEnter());
            userService.getCurrentNode().setPreviousNode(event.getToEnter());

            // Update the new node
            ChordInternalMessage messageN = new ChordInternalMessage(MessageType.UpdateNeighbors, currentNodeDTO, currentNodeDTO); // prev and next are the current node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), messageN);
            
            // Update the finger tables
            ChordInternalMessage messageFT = new ChordInternalMessage(MessageType.UpdateFingerTable, currentNodeDTO, 0);
            System.out.println(messageFT.toString());
            userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), messageFT);

            // TODO: update all the finger tables

        } else if (nodeWithHashDTO == null) { // target node (Prev to the new Node) is the current node
            // Update the neighbors
            userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, event.getToEnter())); // mudar prev do next para o novo node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), (NodeDTO) null)); // mudar next do novo node para o next do current
            currentNode.setNextNode(nodeToEnterDTO);// mudar next do current para o novo node
            userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, currentNodeDTO)); // mudar prev do novo node para o current

            // Update the finger tables
            ChordInternalMessage messageFT = new ChordInternalMessage(MessageType.UpdateFingerTable, currentNodeDTO, 0);
            System.out.println(messageFT.toString());
            userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), messageFT);
        
        } else { // foward to the next node
            userService.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage());
        }
    }

    public synchronized void exitNode() { // What about the threads?
        NodeDTO prevNodeDTO = currentNode.getPreviousNode();
        NodeDTO nextNodeDTO = currentNode.getNextNode();

        if (prevNodeDTO == null && nextNodeDTO == null) // the current node is the only node in the network (default node) 
            return;
        
        // mudar next do prev para o next do current
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), (NodeDTO) null);
        userService.startClient(currentNode.getPreviousNode().getIp(), currentNode.getPreviousNode().getPort(), message);
        
        // mudar prev do next para o prev do current
        ChordInternalMessage message2 = new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, currentNode.getPreviousNode());
        userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), message2);

        // Update the finger tables
        ChordInternalMessage message3 = new ChordInternalMessage(MessageType.UpdateFingerTable, currentNodeDTO, 0);
        userService.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), message3);
    }

    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate(); // Node that started the event
        ArrayList<NodeDTO> fingerTable = userService.getCurrentNode().getFingerTable();
        int ringSize = userService.getRingSize();
    
        if (currentNodeDTO.equals(nodeToUpdateDTO)) {
            userService.getCurrentNode().setFingerTable(message.getFingerTable());
            return;
        } else if (counter == userService.getHashLength()) { // No more nodes to add
            userService.startClient(nodeToUpdateDTO.getIp(), nodeToUpdateDTO.getPort(), message); // Send the message back to the node that started the event
            return;
        }
    
        BigInteger nodeToUpdateDTOIndex = nodeToUpdateDTO.getHash();
        BigInteger currentNodeDTOIndex = currentNodeDTO.getHash();
        BigInteger ringSizeBig = BigInteger.valueOf(ringSize);
    
        BigInteger distance = nodeToUpdateDTOIndex.subtract(currentNodeDTOIndex).add(ringSizeBig).mod(ringSizeBig);
        BigInteger twoPowerCounter = BigInteger.valueOf(2).pow(counter);
    
        if (distance.compareTo(twoPowerCounter) >= 0) {
            message.addNodeToFingerTable(currentNodeDTO);
    
            // Skip the next counters if 2^counter is lower than the distance between this and the next node to try
            while (twoPowerCounter.compareTo(distance) <= 0) {
                counter++;
                twoPowerCounter = BigInteger.valueOf(2).pow(counter); // Update twoPowerCounter for the new value of counter
                message.incCounter();
            }
        }
    
        NodeDTO nextNode = currentNode.getNextNode();
        userService.startClient(nextNode.getIp(), nextNode.getPort(), message);
    }
    
    
    public synchronized void broadcastMessage(BroadcastUpdateFingerTableEvent event) {
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateFingerTable, currentNodeDTO, 0);
        updateFingerTable(new UpdateNodeFingerTableEvent(message));
        if (!event.getInitializer().equals(currentNodeDTO)) { // foward to the next node
            NodeDTO nextNodeDTO = currentNode.getNextNode();
            userService.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), event.getMessage());
        }
    }
}
