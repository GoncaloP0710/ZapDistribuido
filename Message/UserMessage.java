package Message;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.security.PublicKey;

import dtos.NodeDTO;

public class UserMessage extends Message {

    private byte[] messageEncryp;
    private LocalDateTime time;
    private NodeDTO senderDTO;
    private BigInteger receiverHash;
    private NodeDTO receiverDTO; // Only known once the message is received
    private boolean needConfirmation;
    private byte[] messageHash;
    private boolean directMessage; // If the message is a direct message, it is double encrypted. Use only the ssl/tls encryption

    public UserMessage(MessageType messageType, NodeDTO senderDTO, BigInteger receiverHash, byte[] messageEncryp, boolean needConfirmation, byte[] messageHash, boolean directMessage) {
        super(messageType);
        this.receiverHash = receiverHash;
        this.senderDTO = senderDTO;
        this.time = LocalDateTime.now();
        this.messageEncryp = messageEncryp;
        this.needConfirmation = needConfirmation;
        this.messageHash = messageHash;
        this.directMessage = directMessage;
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

    public boolean getNeedConfirmation(){
        return this.needConfirmation;
    }

    public byte[] getMessageHash(){
        return this.messageHash;
    }

    public boolean getDirectMessage(){
        return this.directMessage;
    }

    public void setMessageEncryp(byte[] messageEncryp){
        this.messageEncryp = messageEncryp;
    }

    public void setMessageHash(byte[] messageHash){
        this.messageHash = messageHash;
    }
}
