package Message;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.security.PublicKey;

import dtos.UserDTO;

public class UserMessage extends Message {

    private byte[] messageEncryp;
    PublicKey receiverPubKey;
    private LocalDateTime time;
    private UserDTO senderDTO;
    private BigInteger receiverHash;
    private UserDTO receiverDTO; // Only known once the message is received

    public UserMessage(MessageType messageType, UserDTO senderDTO, BigInteger receiverHash, byte[] messageEncryp) {
        super(messageType);
        this.receiverHash = receiverHash;
        this.senderDTO = senderDTO;
        this.time = LocalDateTime.now();
        this.messageEncryp = messageEncryp;

    }

    public UserDTO getReceiverDTO(){
        return this.receiverDTO;
    }

    public void setReceiverDTO(UserDTO receiverDTO){
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
}
