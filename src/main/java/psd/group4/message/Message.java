package psd.group4.message;

import java.io.Serializable;

public abstract class Message implements Serializable {

    private MessageType msgType;

    public Message(MessageType messageType){
        this.msgType = messageType;
    }

    public MessageType getMsgType(){
        return this.msgType;
    }

}
