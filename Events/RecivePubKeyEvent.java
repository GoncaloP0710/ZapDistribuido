package Events;

import java.security.PublicKey;
import java.lang.classfile.components.ClassPrinter.Node;
import java.math.BigInteger;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class RecivePubKeyEvent extends NodeEvent {

    private NodeDTO initializer; 
    private PublicKey receiverPubKey;
    private BigInteger target;
    private NodeDTO targetDTO;

    public RecivePubKeyEvent(ChordInternalMessage msg) {
		super(msg);
        this.initializer = msg.getInitializer();
        this.receiverPubKey = msg.getReceiverPubKey();
        this.target = msg.getTarget();
        this.targetDTO = msg.getTargetDTO();
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

    public NodeDTO getTargetDTO() {
        return targetDTO;
    }

}
