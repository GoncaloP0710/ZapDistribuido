package psd.group4.message;


public enum MessageType {
    EnterNode,
    ExitNode,
    UpdateFingerTable,
    SendMsg,
    UpdateNeighbors,
    broadcastUpdateFingerTable,
    RecivePubKey,
    addCertificateToTrustStore,
    diffHellman,
    Notify,
    RemoveSharedKey,
    SendGroupMsg,
    AddUserToGroup,
}
