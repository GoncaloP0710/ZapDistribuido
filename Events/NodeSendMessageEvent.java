package Events;

import java.math.BigInteger;

import client.Node;

public class NodeSendMessageEvent extends NodeEvent {

    private String message;
    private BigInteger reciver;

    public NodeSendMessageEvent(Node current, String message, BigInteger reciver) {
        super(current);
        this.message = message;
        this.reciver = reciver;
    }

}
