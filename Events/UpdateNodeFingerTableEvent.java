package Events;

import client.Node;

// TODO: chose a way to say if the update is to a specific node or to all nodes
public class UpdateNodeFingerTableEvent extends NodeEvent { 

    public UpdateNodeFingerTableEvent(Node current) {
        super(current);
    }

}
