package Message;

import java.io.Serializable;
import java.math.BigInteger;

import dtos.UserDTO;

public abstract class Message implements Serializable {

    private MessageType msgType;
    private UserDTO senderDTO;
    private BigInteger reciverHash;

    public Message(MessageType messageType, UserDTO senderDTO, BigInteger reciverHash){
        this.msgType = messageType;
        this.senderDTO = senderDTO;
        this.reciverHash = reciverHash;
    }

    public MessageType getMsgType(){
        return this.msgType;
    }

    public UserDTO getSenderDTO(){
        return this.senderDTO;
    }

    public BigInteger getReciverHash(){
        return this.reciverHash;
    }
}
