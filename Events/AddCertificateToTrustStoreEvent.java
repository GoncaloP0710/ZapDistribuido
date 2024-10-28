package Events;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

import java.security.PublicKey;

public class AddCertificateToTrustStoreEvent extends NodeEvent {
    private final String aliasReciver;
    private final String aliasSender;
    private byte[] certificateInitializer;
    private byte[] certificateReciver;
    private PublicKey initializerPublicKey;
    private PublicKey targetPublicKey;

    private NodeDTO initializer; // The last node that sent the message (not necessarily the one that started the event)

    public AddCertificateToTrustStoreEvent(ChordInternalMessage msg) {
        super(msg);
        this.aliasReciver = msg.getAliasReciver();
        this.aliasSender = msg.getAliasSender();
        this.certificateInitializer = msg.getCertificateInitializer();
        this.certificateReciver = msg.getCertificateReciver();
        this.initializer = msg.getInitializer();
        this.initializerPublicKey = msg.getInitializerPublicKey();
        this.targetPublicKey = msg.getTargetPublicKey();
    }

    public String getAliasReciver() {
        return aliasReciver;
    }

    public String getAliasSender() {
        return aliasSender;
    }

    public byte[] getCertificateInitializer() {
        return certificateInitializer;
    }

    public byte[] getCertificateReciver() {
        return certificateReciver;
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public PublicKey getInitializerPublicKey() {
        return initializerPublicKey;
    }

    public PublicKey getTargetPublicKey() {
        return targetPublicKey;
    }
}