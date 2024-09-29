package Events;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class BroadcastUpdateFingerTableEvent extends NodeEvent {

    private NodeDTO initializer;
    private boolean finishedBroadcasting;

    public BroadcastUpdateFingerTableEvent(ChordInternalMessage msg) {
        super(msg);
        this.initializer = msg.getInitializer();
        this.finishedBroadcasting = msg.getFinishedBroadcasting();
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public boolean getFinishedBroadcasting() {
        return finishedBroadcasting;
    }

    public void setFinishedBroadcasting(boolean finishedBroadcasting) {
        this.finishedBroadcasting = finishedBroadcasting;
    }
}
