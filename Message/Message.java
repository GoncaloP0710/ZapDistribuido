package Message;

import java.io.Serializable;
import java.math.BigInteger;

import dtos.NodeDTO;

public abstract class Message implements Serializable {

    private MessageType msgType;
    private BigInteger reciverHash;

    public Message(MessageType messageType, BigInteger reciverHash){
        this.msgType = messageType;
        this.reciverHash = reciverHash;
    }

    public MessageType getMsgType(){
        return this.msgType;
    }

    public BigInteger getReciverHash(){
        return this.reciverHash;
    }

}
