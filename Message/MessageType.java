package Message;

import Events.RecivePubKeyEvent;

public enum MessageType {
    EnterNode,
    ExitNode,
    UpdateFingerTable,
    SendMsg,
    UpdateNeighbors,
    broadcastUpdateFingerTable,
    RecivePubKeyEvent;
}
