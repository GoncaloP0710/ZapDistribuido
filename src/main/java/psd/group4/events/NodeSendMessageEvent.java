package psd.group4.events;

import java.math.BigInteger;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.UserMessage;

public class NodeSendMessageEvent extends NodeEvent {

    private byte[] messageEncryp;
    private BigInteger receiver;
    private NodeDTO senderDTO;
    private boolean needConfirmation;
    private byte[] messageHash;
    private boolean directMessage;

    public NodeSendMessageEvent(UserMessage msg) {
        super(msg);
        this.messageEncryp = msg.getMessageEncryp();
        this.receiver = msg.getreceiverHash();
        this.senderDTO = msg.getSenderDTO();
        this.needConfirmation = msg.getNeedConfirmation();
        this.messageHash = msg.getMessageHash();
        this.directMessage = msg.getDirectMessage();
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

    public boolean getNeedConfirmation() {
        return this.needConfirmation;
    }

    public byte[] getMessageHash() {
        return this.messageHash;
    }

    public boolean getDirectMessage() {
        return this.directMessage;
    }

}
