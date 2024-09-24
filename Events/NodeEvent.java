package Events;

import utils.observer.Event;
import client.Node;

public abstract class NodeEvent implements Event {
    
    private Node node;

    public NodeEvent(Node node){
        this.node = node;
    }

    public Node getNode(){
        return this.node;
    }
}
