package psd.group4.client;

import java.security.SecureRandom;
import java.util.*;

public class MessageEntry {
    private byte[] sender; 
    private byte[] receiver; 
    private byte[] message; 
    private Date date;
    private long id;
    private int bitLength;
    private SecureRandom random;

    public MessageEntry(){}

    public MessageEntry(byte[] sender, byte[] receiver, byte[] message, Date date){
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.date = date;
    }

    public MessageEntry(byte[] sender, byte[] receiver, byte[] message, Date date, long id, int bitLength, SecureRandom random){
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.date = date;
        this.id = id;
        this.bitLength = bitLength;
        this.random = random;
    }

    public MessageEntry(byte[] message, MessageEntry me){
        this.message = message;

        this.sender = me.getSender();
        this.receiver = me.getReceiver();
        this.date = me.getDate();
        this.id = me.getId();
        this.bitLength = me.getBitLength();
        this.random = me.getRandom();
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

    public long getId() {
        return id;
    }

    public int getBitLength() {
        return bitLength;
    }

    public SecureRandom getRandom() {
        return random;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public void setBitLength(int bitLength) {
        this.bitLength = bitLength;
    }
    
    public void setRandom(SecureRandom random) {
        this.random = random;
    }
    
    public void setId(long id) {
        this.id = id;
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
        StringBuilder s = new StringBuilder("Id: "+ id + "\n");
        s.append("Sender: "+ sender + "\n");
        s.append("Receiver: "+ receiver + "\n");
        s.append("Message: "+ message + "\n");
        s.append("Date: "+ date.toString() + "\n");
        return s.toString();
    }
}