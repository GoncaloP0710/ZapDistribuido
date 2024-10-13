package Events;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class BroadcastUpdateFingerTableEvent extends NodeEvent {

    private NodeDTO initializer; // The node that first started the broadcast
    private NodeDTO senderDto; // The sender of the message
    private boolean finishedBroadcasting;

    public BroadcastUpdateFingerTableEvent(ChordInternalMessage msg) {
        super(msg);
        this.initializer = msg.getInitializer();
        this.senderDto = msg.getSenderDto();
        this.finishedBroadcasting = msg.getFinishedBroadcasting();
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public NodeDTO getSenderDto() {
        return senderDto;
    }

    public boolean getFinishedBroadcasting() {
        return finishedBroadcasting;
    }

    public void setFinishedBroadcasting(boolean finishedBroadcasting) {
        this.finishedBroadcasting = finishedBroadcasting;
    }
}
