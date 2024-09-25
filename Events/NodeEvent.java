package Events;

import utils.observer.Event;
import client.Node;

public abstract class NodeEvent implements Event {
    
    private Node node;

    public NodeEvent(Node node){ // TODO: Probably remove the Node parameter
        this.node = node;
    }

    public Node getNode(){
        return this.node;
    }
}
