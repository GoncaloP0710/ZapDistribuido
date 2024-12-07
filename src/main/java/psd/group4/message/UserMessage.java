package psd.group4.message;

import java.math.BigInteger;
import java.time.LocalDateTime;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import psd.group4.dtos.NodeDTO;

public class UserMessage extends Message {

    private byte[] messageEncryp;
    private LocalDateTime time;
    private NodeDTO senderDTO;
    private BigInteger receiverHash;
    private NodeDTO receiverDTO; // Only known once the message is received
    private boolean needConfirmation;
    private byte[] messageHash;
    private boolean directMessage; // If the message is a direct message, it is double encrypted. Use only the ssl/tls encryption

    private String groupName;
    private byte[] messageEncrypGroup;

    /**
     * Normal message
     */
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

    /**
     * Group Message
     */
    public UserMessage(MessageType messageType, NodeDTO senderDTO, byte[] messageEncrypGroup, byte[] messageHash, String groupName) {
        super(messageType);
        this.senderDTO = senderDTO;
        this.time = LocalDateTime.now();
        this.messageEncrypGroup = messageEncrypGroup;
        this.messageHash = messageHash;
        this.groupName = groupName;
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

    public byte[] getMessageEncrypGroup(){
        return this.messageEncrypGroup;
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

    public String getGroupName(){
        return this.groupName;
    }
}
