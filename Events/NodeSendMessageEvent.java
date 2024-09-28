package Events;

import java.math.BigInteger;

import Message.Message;

public class NodeSendMessageEvent extends NodeEvent {

    private byte[] message;
    private BigInteger reciver;

    public NodeSendMessageEvent(Message msg, byte[] message, BigInteger reciver) {
        super(msg);
        this.message = message;
        this.reciver = reciver;
    }

}
