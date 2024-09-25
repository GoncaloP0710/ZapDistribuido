package handlers;

import Events.*;
import client.UserService;
import client.UserService;

public class EventHandler {

    private UserService nodeService;

    public EventHandler(UserService nodeService) {
        this.nodeService = nodeService;
    }

    public void enterNode(EnterNodeEvent event) {
        
    }
}
