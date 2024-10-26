package Events;

import java.math.BigInteger;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class DiffHellmanEvent extends NodeEvent {
    private byte[] key;
    private NodeDTO initializer;
    private BigInteger target;


    public DiffHellmanEvent(ChordInternalMessage msg) {
        super(msg);
        this.key = msg.getSharedKey();
        this.initializer = msg.getInitializer();
        this.target = msg.getTarget();
    }

    public byte[] getSharedKey() {
        return key;
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public BigInteger getTarget() {
        return target;
    }
}