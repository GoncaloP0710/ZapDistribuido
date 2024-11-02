package psd.group4.events;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.ChordInternalMessage;

public class UpdateNodeFingerTableEvent extends NodeEvent { 

    private NodeDTO nodeToUpdate;
    private int counter;

    public UpdateNodeFingerTableEvent(ChordInternalMessage msg) {
        super(msg);
        counter = msg.getCounter();
        this.nodeToUpdate = msg.getNodeToUpdate();
    }

    public NodeDTO getNodeToUpdate() {
        return nodeToUpdate;
    }

    public int getCounter() {
        return counter;
    }

}
