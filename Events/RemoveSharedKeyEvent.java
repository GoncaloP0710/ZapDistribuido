package Events;

import dtos.NodeDTO;
import Message.ChordInternalMessage;

import java.math.BigInteger;

public class RemoveSharedKeyEvent extends NodeEvent {

    private NodeDTO initializer;
    private BigInteger target;

    public RemoveSharedKeyEvent(ChordInternalMessage msg) {
        super(msg);
        this.initializer = msg.getInitializer();
        this.target = msg.getTarget();
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public BigInteger getTarget() {
        return target;
    }
}