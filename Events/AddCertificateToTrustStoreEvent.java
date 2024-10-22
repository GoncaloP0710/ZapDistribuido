package Events;

import Message.ChordInternalMessage;
import java.security.cert.Certificate;

public class AddCertificateToTrustStoreEvent extends NodeEvent {
    private final String alias;
    private Certificate certificate;

    public AddCertificateToTrustStoreEvent(ChordInternalMessage msg) {
        super(msg);
        this.alias = msg.getUsername();
        this.certificate = msg.getCertificate();
    }

    public String getAlias() {
        return alias;
    }

    public Certificate getCertificate() {
        return certificate;
    }
}