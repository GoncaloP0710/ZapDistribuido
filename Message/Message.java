package Message;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

import client.User;
import utils.*;
import dtos.UserDTO;

public abstract class Message implements Serializable {

    private MessageType msgType;
    private MessageStatus msgStatus;
    private LocalDateTime time;
    private UserDTO senderDTO;
    private BigInteger reciverHash;
    private UserDTO receiverDTO; // Only known once the message is received

    public Message(MessageType messageType, UserDTO senderDTO, BigInteger reciverHash){
        this.msgType = messageType;
        this.msgStatus = MessageStatus.SENDING;
        this.time = LocalDateTime.now();
        this.senderDTO = senderDTO;
        this.reciverHash = reciverHash;
    }

    public MessageType getMsgType(){
        return this.msgType;
    }

    public MessageStatus getMsgStatus(){
        return this.msgStatus;
    }

    public LocalDateTime getTime(){
        return this.time;
    }

    public UserDTO getSenderDTO(){
        return this.senderDTO;
    }

    public BigInteger getReciverHash(){
        return this.reciverHash;
    }

    public UserDTO getReceiverDTO(){
        return this.receiverDTO;
    }

    public void setReceiverDTO(UserDTO receiverDTO){
        this.receiverDTO = receiverDTO;
    }
}
