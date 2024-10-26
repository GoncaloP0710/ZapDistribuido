package Message;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.security.PublicKey;

import dtos.NodeDTO;

public class UserMessage extends Message {

    private byte[] messageEncryp;
    PublicKey receiverPubKey;
    private LocalDateTime time;
    private NodeDTO senderDTO;
    private BigInteger receiverHash;
    private NodeDTO receiverDTO; // Only known once the message is received
    private boolean needConfirmation;

    public UserMessage(MessageType messageType, NodeDTO senderDTO, BigInteger receiverHash, byte[] messageEncryp, boolean needConfirmation) {
        super(messageType);
        this.receiverHash = receiverHash;
        this.senderDTO = senderDTO;
        this.time = LocalDateTime.now();
        this.messageEncryp = messageEncryp;
        this.needConfirmation = needConfirmation;
    }

    public LocalDateTime getTime(){
        return this.time;
    }

    public NodeDTO getSenderDTO(){
        return this.senderDTO;
    }

    public NodeDTO getReceiverDTO(){
        return this.receiverDTO;
    }

    public void setReceiverDTO(NodeDTO receiverDTO){
        this.receiverDTO = receiverDTO;
    }

    public byte[] getMessageEncryp(){
        return this.messageEncryp;
    }

    public BigInteger getreceiverHash(){
        return this.receiverHash;
    }

    public PublicKey getreceiverPubKey(){
        return this.receiverPubKey;
    }

    public void setreceiverPubKey(PublicKey receiverPubKey){
        this.receiverPubKey = receiverPubKey;
    }

    public boolean getNeedConfirmation(){
        return this.needConfirmation;
    }
}
