package Events;

import java.math.BigInteger;

import Message.UserMessage;

public class NodeSendMessageEvent extends NodeEvent {

    private byte[] message;
    private BigInteger reciver;
    // TODO: Add the rest of the attributes needed to be passed to the event

    public NodeSendMessageEvent(UserMessage msg) {
        super(msg);
        this.message = msg.getMessage();
        this.reciver = msg.getReciverHash();
    }

    public byte[] getUserMessage() {
        return this.message;
    }

    public BigInteger getReciver() {
        return this.reciver;
    }

}
