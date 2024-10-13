package Handlers;

import java.math.BigInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Events.*;
import Message.*;
import Client.Node;
import Client.UserService;
import dtos.*;

public class EventHandler { 

    private UserService userService;

    String ipDefault;
    int portDefault;
    boolean isDefaultNode;
    NodeDTO currentNodeDTO;
    Node currentNode;

    // private final Lock updateNeighborsLock = new ReentrantLock();
    private final Lock enterNodeLock = new ReentrantLock();

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
        if (event.getNext() != null) {
            currentNode.setNextNode(event.getNext());
            System.out.println("Next node: " + event.getNext().toString());
        }
        if (event.getPrevious() != null) {
            currentNode.setPreviousNode(event.getPrevious());
            System.out.println("Previous node: " + event.getPrevious().toString());
        }
    }

    public void enterNode(EnterNodeEvent event) {
        enterNodeLock.lock();
        try {
            BigInteger hash = event.getToEnterHash();
            NodeDTO nodeToEnterDTO = event.getToEnter();
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);

            if (nodeWithHashDTO == null) { // target node (Prev to the new Node) is the current node
                // Update the neighbors
                userService.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, event.getToEnter()), true); // mudar prev do next para o novo node
                userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), (NodeDTO) null), true); // mudar next do novo node para o next do current
                currentNode.setNextNode(nodeToEnterDTO);// mudar next do current para o novo node
                userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, currentNodeDTO), true); // mudar prev do novo node para o current
                
                // Update all the finger tables
                userService.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, currentNodeDTO, currentNodeDTO), true);

            } else { // foward to the closest node in the finger table of the current node to the new node
                userService.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false);
            }
        } finally {
            enterNodeLock.unlock();
        }
    }

    public synchronized void exitNode() {
        NodeDTO prevNodeDTO = currentNode.getPreviousNode();
        NodeDTO nextNodeDTO = currentNode.getNextNode();
        
        // mudar next do prev para o next do current
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateNeighbors, nextNodeDTO, (NodeDTO) null);
        userService.startClient(prevNodeDTO.getIp(), prevNodeDTO.getPort(), message, true);
        
        // mudar prev do next para o prev do current
        ChordInternalMessage message2 = new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, prevNodeDTO);
        userService.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), message2, true);

        // Update all the finger tables | Next e mandas o current
        userService.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, prevNodeDTO, prevNodeDTO), true);

    }

    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate(); // Node that started the event
        int ringSize = userService.getRingSize();
    
        if (currentNodeDTO.equals(nodeToUpdateDTO)) {
            userService.getCurrentNode().setFingerTable(message.getFingerTable());
            return;
        } else if (counter == userService.getHashLength()) { // No more nodes to add
            userService.startClient(nodeToUpdateDTO.getIp(), nodeToUpdateDTO.getPort(), message, true); // Send the message back to the node that started the event
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
        userService.startClient(nextNode.getIp(), nextNode.getPort(), message, false);
    }
    
    public synchronized void broadcastMessage(BroadcastUpdateFingerTableEvent event) {
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateFingerTable, event.getSenderDto(), 0);
        updateFingerTable(new UpdateNodeFingerTableEvent(message)); 

        if (!event.getInitializer().equals(currentNodeDTO)) { // foward to the next node
            NodeDTO nextNodeDTO = currentNode.getNextNode();
            ((ChordInternalMessage) event.getMessage()).setSenderDto(currentNodeDTO);
            userService.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), event.getMessage(), false);
        }
    }
}