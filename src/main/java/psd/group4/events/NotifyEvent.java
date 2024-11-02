package psd.group4.events;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.ChordInternalMessage;

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