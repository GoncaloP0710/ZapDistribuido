package Events;

import java.math.BigInteger;

import Message.UserMessage;
import dtos.NodeDTO;

public class NodeSendMessageEvent extends NodeEvent {

    private byte[] messageEncryp;
    private BigInteger receiver;
    private NodeDTO senderDTO;

    public NodeSendMessageEvent(UserMessage msg) {
        super(msg);
        this.messageEncryp = msg.getMessageEncryp();
        this.receiver = msg.getreceiverHash();
        this.senderDTO = msg.getSenderDTO();
    }

    public BigInteger getReciver() {
        return this.receiver;
    }

    public byte[] getMessageEncryp() {
        return this.messageEncryp;
    }

    public NodeDTO getSenderDTO() {
        return this.senderDTO;
    }

}
