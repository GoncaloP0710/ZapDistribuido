package handlers;

import Events.*;

public class EventHandler {

    private NodeService nodeService;

    public EventHandler(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void enterNode(EnterNodeEvent event) {
        
    }
}
