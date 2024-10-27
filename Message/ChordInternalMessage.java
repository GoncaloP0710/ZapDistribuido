package Message;

import java.util.ArrayList;

import Client.Node;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.math.BigInteger;

import dtos.NodeDTO;

public class ChordInternalMessage extends Message {

    private NodeDTO nextNode; // UpdateNeighbors Event
    private NodeDTO previousNode; // UpdateNeighbors Event
    private NodeDTO nodeToEnter; // EnterNode Event
    private NodeDTO nodeToUpdate; // UpdateNodeFingerTableEvent
    private int counter; // UpdateNodeFingerTableEvent
    private ArrayList<NodeDTO> fingerTable; // UpdateNodeFingerTableEvent
    private NodeDTO initializer; // BroadcastUpdateFingerTableEvent | DiffHellmanEvent | UpdateNeighbors Event
    private NodeDTO senderDto; // BroadcastUpdateFingerTableEvent 
    private boolean finishedBroadcasting; // BroadcastUpdateFingerTableEvent
    private BigInteger target; // DiffHellmanEvent | NotifyEvent
    private String aliasReciver; // AddCertificateToTrustStoreEvent
    private String aliasSender; // AddCertificateToTrustStoreEvent
    private Certificate certificate; // AddCertificateToTrustStoreEvent
    private PublicKey initializerPublicKey; // DiffHellmanEvent
    private PublicKey targetPublicKey; // DiffHellmanEvent


    private NodeDTO targetDTO; // 

    // UpdateNeighboringNodesEvent
    public ChordInternalMessage(MessageType messageType, NodeDTO nextNode, NodeDTO previousNode, NodeDTO initializer) {
        super(messageType);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
        this.initializer = initializer;
    }

    // EnterNodeEvent
    public ChordInternalMessage(MessageType messageType, NodeDTO nodeToEnter) {
        super(messageType);
        this.nodeToEnter = nodeToEnter;
    }
    
    /**
     * UpdateNodeFingerTableEvent
     * 
     * @param nodeToUpdate this is the that first started the event
     * @requires counter == 0
     */
    public ChordInternalMessage(MessageType messageType, NodeDTO nodeToUpdate, int counter) {
        super(messageType);
        this.nodeToUpdate = nodeToUpdate;
        this.counter = counter;
        this.fingerTable = new ArrayList<>();
    }

    /**
     * BroadcastUpdateFingerTableEvent
     * @requires finishedBroadcasting == false
     */
    public ChordInternalMessage(MessageType messageType, Boolean finishedBroadcasting, NodeDTO initializer, NodeDTO senderDto) {
        super(messageType);
        this.initializer = initializer;
        this.senderDto = senderDto;
        this.finishedBroadcasting = finishedBroadcasting;
    }

    /**
     * AddCertificateToTrustStoreEvent
     */
    public ChordInternalMessage(MessageType messageType, Certificate certificate, String aliasReciver, String aliasSender) {    
        super(messageType);
        this.certificate = certificate;
        this.aliasReciver = aliasReciver;
        this.aliasSender = aliasSender;
    }

    /**
     * DiffHellmanEvent
     */
    public ChordInternalMessage(MessageType messageType, NodeDTO initializer, BigInteger recieverDTO, PublicKey targetPublicKey, PublicKey initializerPublicKey) {
        super(messageType);
        this.initializer = initializer;
        this.target = recieverDTO;
        this.targetPublicKey = targetPublicKey;
        this.initializerPublicKey = initializerPublicKey;
    }

    /**
     * NotifyEvent
     */
    public ChordInternalMessage(MessageType messageType, BigInteger target) {
        super(messageType);
        this.target = target;
    }

    public NodeDTO getNextNode(){
        return this.nextNode;
    }

    public NodeDTO getPreviousNode(){
        return this.previousNode;
    }

    public NodeDTO getNodeToEnter(){
        return this.nodeToEnter;
    }

    public NodeDTO getNodeToUpdate(){
        return this.nodeToUpdate;
    }

    public int getCounter(){
        return this.counter;
    }

    public void incCounter(){
        this.counter++;
    }

    public ArrayList<NodeDTO> getFingerTable(){
        return this.fingerTable;
    }

    public void addNodeToFingerTable(NodeDTO node){
        this.fingerTable.add(node);
    }

    public NodeDTO getInitializer(){
        return this.initializer;
    }

    public NodeDTO getSenderDto(){
        return this.senderDto;
    }

    public void setSenderDto(NodeDTO senderDto){
        this.senderDto = senderDto;
    }

    public boolean getFinishedBroadcasting(){
        return this.finishedBroadcasting;
    }

    public BigInteger getTarget(){
        return this.target;
    }

    public void setTarget(BigInteger target){
        this.target = target;
    }

    public String getAliasReciver(){
        return this.aliasReciver;
    }

    public String getAliasSender(){
        return this.aliasSender;
    }

    public Certificate getCertificate(){
        return this.certificate;
    }

    public NodeDTO getTargetDTO(){
        return this.targetDTO;
    }

    public void setTargetDTO(NodeDTO targetDTO){
        this.targetDTO = targetDTO;
    }

    public void setCertificate(Certificate certificate){
        this.certificate = certificate;
    }

    public PublicKey getInitializerPublicKey(){
        return this.initializerPublicKey;
    }

    public PublicKey getTargetPublicKey(){
        return this.targetPublicKey;
    }

    public void setTargetPublicKey(PublicKey targetPublicKey){
        this.targetPublicKey = targetPublicKey;
    }

    public void setInitializerPublicKey(PublicKey initializerPublicKey){
        this.initializerPublicKey = initializerPublicKey;
    }
}
