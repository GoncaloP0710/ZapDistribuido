package Events;

import Message.Message;

// TODO: chose a way to say if the update is to a specific node or to all nodes
public class UpdateNodeFingerTableEvent extends NodeEvent { 

    public UpdateNodeFingerTableEvent(Message msg) {
        super(msg);
    }

}
