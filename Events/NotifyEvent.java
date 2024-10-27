package Events;

import java.math.BigInteger;

import Message.ChordInternalMessage;

public class NotifyEvent extends NodeEvent {

    private BigInteger target;

    public NotifyEvent(ChordInternalMessage msg) {
        super(msg);
        this.target = msg.getTarget();
    }

    public BigInteger getTarget() {
        return target;
    }
}