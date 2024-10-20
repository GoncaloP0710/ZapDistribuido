package Message;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.security.PublicKey;

import dtos.UserDTO;

public class UserMessage extends Message {

    private byte[] messageEncrypSender;
    private byte[] messageEncrypReceiver;
    PublicKey receiverPubKey;
    private LocalDateTime time;
    private UserDTO senderDTO;
    private BigInteger receiverHash;
    private UserDTO receiverDTO; // Only known once the message is received

    public UserMessage(MessageType messageType, UserDTO senderDTO, BigInteger receiverHash, byte[] messageEncrypSender, byte[] messageEncrypReceiver) {
        super(messageType);
        this.receiverHash = receiverHash;
        this.senderDTO = senderDTO;
        this.time = LocalDateTime.now();
        this.messageEncrypSender = messageEncrypSender;
        this.messageEncrypReceiver = messageEncrypReceiver;
    }

    public UserDTO getReceiverDTO(){
        return this.receiverDTO;
    }

    public void setReceiverDTO(UserDTO receiverDTO){
        this.receiverDTO = receiverDTO;
    }

    public byte[] getMessageEncrypSender(){
        return this.messageEncrypSender;
    }

    public byte[] getMessageEncrypReceiver(){
        return this.messageEncrypReceiver;
    }

    public BigInteger getreceiverHash(){
        return this.receiverHash;
    }

    public PublicKey getreceiverPubKey(){
        return this.receiverPubKey;
    }

    public void setMessageEncrypreceiver(byte[] messageEncrypReceiver){
        this.messageEncrypReceiver = messageEncrypReceiver;
    }

    public void setreceiverPubKey(PublicKey receiverPubKey){
        this.receiverPubKey = receiverPubKey;
    }
}
