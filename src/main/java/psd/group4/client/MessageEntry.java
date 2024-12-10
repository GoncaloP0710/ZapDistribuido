package psd.group4.client;

import java.io.Serializable;
import java.util.*;

import org.bson.types.ObjectId;

public class MessageEntry implements Serializable {
    private ObjectId _id;
    // @BsonProperty(value = "_id")
    private byte[] sender; 
    private byte[] receiver; 
    private Date date;
    private long identifier;
    private String field;
    private String share;
    private String shareHolder;


    public MessageEntry(){
        this._id = new ObjectId(); // Generate a unique _id
    }

    public MessageEntry(byte[] sender, byte[] receiver, Date date, String shareHolder, String share){
        this._id = new ObjectId(); // Generate a unique _id
        this.sender = sender;
        this.receiver = receiver;
        this.share = share;
        this.shareHolder = shareHolder;
        this.date = date;
    }

    public MessageEntry(byte[] sender, byte[] receiver, Date date, long id, String field, String shareHolder, String share){
        this._id = new ObjectId(); // Generate a unique _id
        this.sender = sender;
        this.receiver = receiver;
        this.date = date;
        this.identifier = id;
        this.field = field;
        this.share = share;
        this.shareHolder = shareHolder;
    }

    public MessageEntry(String shareHolder, String share, MessageEntry me){
        this._id = new ObjectId(); // Generate a unique _id
        this.share = share;
        this.shareHolder = shareHolder;
        this.sender = me.getSender();
        this.receiver = me.getReceiver();
        this.date = me.getDate();
        this.identifier = me.getIdentifier();
        this.field = me.getField();
        
    }

    public byte[] getSender() {
        return sender;
    }

    public byte[] getReceiver() {
        return receiver;
    }

    public Date getDate() {
        return date;
    }

    public long getIdentifier() {
        return identifier;
    }

    public String getField() {
        return field;
    }

    public String getShare() {
        return share;
    }

    public String getShareHolder() {
        return shareHolder;
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
    
    public void setField(String field) {
        this.field = field.toString();
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

    public void setShare(String share) {
        this.share = share;
    }

    public void setShareHolder(String shareHolder) {
        this.shareHolder = shareHolder;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder("Id: "+ identifier + "\n");
        s.append("Sender: "+ sender + "\n");
        s.append("Receiver: "+ receiver + "\n");
        s.append("Message: "+ share + "\n");
        // s.append("Share Holder: "+ shareHolder + "\n");
        // s.append("Field: "+ field + "\n");
        s.append("Date: "+ date.toString() + "\n");
        return s.toString();
    }
}