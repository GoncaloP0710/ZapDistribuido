package Events;

import Message.ChordInternalMessage;

import java.security.cert.Certificate;

public class AddCertificateToTrustStoreEvent extends NodeEvent {
    private final String aliasReciver;
    private final String aliasSender;
    private Certificate certificate;

    public AddCertificateToTrustStoreEvent(ChordInternalMessage msg) {
        super(msg);
        this.aliasReciver = msg.getAliasReciver();
        this.aliasSender = msg.getAliasSender();
        this.certificate = msg.getCertificate();
    }

    public String getAliasReciver() {
        return aliasReciver;
    }

    public String getAliasSender() {
        return aliasSender;
    }

    public Certificate getCertificate() {
        return certificate;
    }
}