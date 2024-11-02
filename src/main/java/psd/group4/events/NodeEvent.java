package psd.group4.events;

import psd.group4.utils.observer.Event;
import psd.group4.message.Message;

public abstract class NodeEvent implements Event {
    
    private Message msg; // Message that invoked the event

    public NodeEvent(Message msg){
        this.msg = msg;
    }

    public Message getMessage(){
        return this.msg;
    }
}
