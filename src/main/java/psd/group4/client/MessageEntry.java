package psd.group4.client;

import java.io.Serializable;
import java.util.*;

import org.bson.types.ObjectId;

public class MessageEntry implements Serializable {
    private ObjectId _id;
    // @BsonProperty(value = "_id")
    private byte[] sender; 
    private byte[] receiver; 
    private byte[] message; 
    private Date date;
    private long identifier;
    private int bitLength;
    public MessageEntry(){
        this._id = new ObjectId(); // Generate a unique _id
    }

    public MessageEntry(byte[] sender, byte[] receiver, byte[] message, Date date){
        this._id = new ObjectId(); // Generate a unique _id
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.date = date;
    }

    public MessageEntry(byte[] sender, byte[] receiver, byte[] message, Date date, long id, int bitLength){
        this._id = new ObjectId(); // Generate a unique _id
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.date = date;
        this.identifier = id;
        this.bitLength = bitLength;
    }

    public MessageEntry(byte[] message, MessageEntry me){
        this._id = new ObjectId(); // Generate a unique _id
        this.message = message;
        this.sender = me.getSender();
        this.receiver = me.getReceiver();
        this.date = me.getDate();
        this.identifier = me.getIdentifier();
        this.bitLength = me.getBitLength();
    }

    public byte[] getSender() {
        return sender;
    }

    public byte[] getReceiver() {
        return receiver;
    }

    public byte[] getMessage() {
        return message;
    }

    public Date getDate() {
        return date;
    }

    public long getIdentifier() {
        return identifier;
    }

    public int getBitLength() {
        return bitLength;
    }

    public ObjectId getId() {
        return _id;
    }

    public void setId(ObjectId id) {
        this._id = id;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public void setBitLength(int bitLength) {
        this.bitLength = bitLength;
    }

    public void setIdentifier(long id) {
        this.identifier = id;
    }

    public void setSender(byte[] sender) {
        this.sender = sender;
    }

    public void setReceiver(byte[] receiver) {
        this.receiver = receiver;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder("Id: "+ identifier + "\n");
        s.append("Sender: "+ sender + "\n");
        s.append("Receiver: "+ receiver + "\n");
        s.append("Message: "+ message + "\n");
        s.append("Date: "+ date.toString() + "\n");
        return s.toString();
    }
}