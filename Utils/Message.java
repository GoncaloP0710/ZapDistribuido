package utils;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    
    private byte[] data;
    private User sender;
    private User receiver;
    private LocalDateTime time;
    private Status status;


    public Message(byte[] data, User sender, User receiver) {
        this.data = data;
        this.sender = sender;
        this.receiver = receiver;
        this.time = LocalDateTime.now();
        this.status = Status.SENDING;
    }

    public byte[] getData(){
        return this.data;
    }

    public User getSender(){
        return this.sender;
    }

    public User getReceiver(){
        return this.receiver;
    }

    public LocalDateTime getTime(){
        return this.time;
    }

    public Status getStatus(){
        return this.status;
    }

    public void setStatus(Status s){
        this.status = s;
    }
}
