package psd.group4.events;

import java.math.BigInteger;
import java.security.PublicKey;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.ChordInternalMessage;

public class DiffHellmanEvent extends NodeEvent {
    private PublicKey initializerPublicKey;
    private PublicKey targetPublicKey;
    private NodeDTO initializer;
    private BigInteger target;

    public DiffHellmanEvent(ChordInternalMessage msg) {
        super(msg);
        this.initializer = msg.getInitializer();
        this.target = msg.getTarget();
        this.initializerPublicKey = msg.getInitializerPublicKey();
        this.targetPublicKey = msg.getTargetPublicKey();
    }

    public PublicKey getInitializerPublicKey() {
        return initializerPublicKey;
    }

    public PublicKey getTargetPublicKey() {
        return targetPublicKey;
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public BigInteger getTarget() {
        return target;
    }
}