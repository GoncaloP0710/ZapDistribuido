package Events;

import Message.Message;
import utils.observer.Event;

public abstract class NodeEvent implements Event {
    
    private Message msg; // Message that invoked the event

    public NodeEvent(Message msg){
        this.msg = msg;
    }

    public Message getMessage(){
        return this.msg;
    }
}
