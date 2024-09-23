package Events;

import utils.observer.Event;
import client.Node;

public abstract class UserEvent implements Event {
    
    private Node node;

    public UserEvent(Node node){
        this.node = node;
    }

    public Node getNode(){
        return this.node;
    }
}
