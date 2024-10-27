package Message;

import java.util.ArrayList;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.lang.classfile.components.ClassPrinter.Node;
import java.math.BigInteger;

import dtos.NodeDTO;

public class ChordInternalMessage extends Message {

    private NodeDTO nextNode; // UpdateNeighbors Event
    private NodeDTO previousNode; // UpdateNeighbors Event
    private NodeDTO nodeToEnter; // EnterNode Event
    private NodeDTO nodeToUpdate; // UpdateNodeFingerTableEvent
    private int counter; // UpdateNodeFingerTableEvent
    private ArrayList<NodeDTO> fingerTable; // UpdateNodeFingerTableEvent
    private NodeDTO initializer; // BroadcastUpdateFingerTableEvent | RecivePubKeyEvent | DiffHellmanEvent
    private NodeDTO senderDto; // BroadcastUpdateFingerTableEvent 
    private boolean finishedBroadcasting; // BroadcastUpdateFingerTableEvent
    private PublicKey receiverPubKey; // RecivePubKeyEvent
    private BigInteger target; // RecivePubKeyEvent | DiffHellmanEvent
    private String aliasReciver; // AddCertificateToTrustStoreEvent
    private String aliasSender; // AddCertificateToTrustStoreEvent
    private Certificate certificate; // AddCertificateToTrustStoreEvent
    private PublicKey initializerPublicKey; // DiffHellmanEvent
    private PublicKey targetPublicKey; // DiffHellmanEvent


    private NodeDTO targetDTO; // RecivePubKeyEvent

    // UpdateNeighboringNodesEvent
    public ChordInternalMessage(MessageType messageType, NodeDTO nextNode, NodeDTO previousNode) {
        super(messageType);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
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
     * RecivePubKeyEvent
     */
    public ChordInternalMessage(MessageType messageType, PublicKey receiverPubKey, BigInteger target, NodeDTO initializer, NodeDTO targetDTO) {    
        super(messageType);
        this.receiverPubKey = receiverPubKey;
        this.target = target;
        this.initializer = initializer;
        this.targetDTO = targetDTO;
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

    public PublicKey getReceiverPubKey(){
        return this.receiverPubKey;
    }

    public BigInteger getTarget(){
        return this.target;
    }

    public void setTarget(BigInteger target){
        this.target = target;
    }

    public void setReceiverPubKey(PublicKey receiverPubKey){
        this.receiverPubKey = receiverPubKey;
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
