package Events;

import java.math.BigInteger;
import java.security.PublicKey;

import Message.UserMessage;

public class NodeSendMessageEvent extends NodeEvent {

    private byte[] messageEncrypSender;
    private byte[] messageEncrypReceiver;
    PublicKey receiverPubKey;
    private BigInteger receiver;

    public NodeSendMessageEvent(UserMessage msg) {
        super(msg);
        this.messageEncrypSender = msg.getMessageEncrypSender();
        this.messageEncrypReceiver = msg.getMessageEncrypReceiver();
        this.receiverPubKey = msg.getreceiverPubKey();
        this.receiver = msg.getreceiverHash();
    }

    public void setPubKey(PublicKey pubKey){
        this.receiverPubKey = pubKey;
    }

    public byte[] getMessageEncrypSender(){
        return this.messageEncrypSender;
    }

    public byte[] getMessageEncrypReceiver(){
        return this.messageEncrypReceiver;
    }

    public PublicKey getReciverPubKey(){
        return this.receiverPubKey;
    }

    public BigInteger getReciver() {
        return this.receiver;
    }

}
