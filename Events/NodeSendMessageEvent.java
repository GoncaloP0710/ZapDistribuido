package Events;

import java.math.BigInteger;

import Message.UserMessage;

public class NodeSendMessageEvent extends NodeEvent {

    private byte[] messageEncryp;
    private BigInteger receiver;

    public NodeSendMessageEvent(UserMessage msg) {
        super(msg);
        this.messageEncryp = msg.getMessageEncryp();
        this.receiver = msg.getreceiverHash();
    }

    public BigInteger getReciver() {
        return this.receiver;
    }

    public byte[] getMessageEncryp() {
        return this.messageEncryp;
    }

}
