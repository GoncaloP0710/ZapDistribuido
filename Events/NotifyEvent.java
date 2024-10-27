package Events;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class NotifyEvent extends NodeEvent {

    private NodeDTO target;

    public NotifyEvent(ChordInternalMessage msg) {
        super(msg);
        this.target = msg.getNodeToEnter();
    }

    public NodeDTO getTarget() {
        return target;
    }
}