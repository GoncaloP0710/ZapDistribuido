package Events;

import java.security.PublicKey;
import java.math.BigInteger;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class RecivePubKeyEvent extends NodeEvent {

    private NodeDTO initializer; 
    private PublicKey receiverPubKey;
    private BigInteger target;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public RecivePubKeyEvent(ChordInternalMessage msg, ObjectInputStream in, ObjectOutputStream out) {
		super(msg);
        this.initializer = msg.getInitializer();
        this.receiverPubKey = msg.getReceiverPubKey();
        this.target = msg.getTarget();
        this.in = in;
        this.out = out;
	}

    public NodeDTO getInitializer() {
        return initializer;
    }

    public PublicKey getReceiverPubKey() {
        return receiverPubKey;
    }

    public BigInteger getTarget() {
        return target;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

}
