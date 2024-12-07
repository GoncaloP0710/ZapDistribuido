package psd.group4.events;

import java.math.BigInteger;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import psd.group4.dtos.NodeDTO;
import psd.group4.message.ChordInternalMessage;

public class AddUserToGroupEvent extends NodeEvent {

    // Critical data
    private byte[] groupAtributesDTOBytesEncrypted;
    private byte[] infoHash;

    // Non critical data
    private BigInteger receiverHash;
    private final PairingKeySerParameter publicKey;
    private NodeDTO senderDTO;

    public AddUserToGroupEvent(ChordInternalMessage msg) {
        super(msg);
        this.publicKey = msg.getPublicKey();
        this.receiverHash = msg.getReceiverHash();
        this.senderDTO = msg.getSenderDto();
        this.groupAtributesDTOBytesEncrypted = msg.getGroupAtributesDTOBytes();
        this.infoHash = msg.getInfoHash();
    }

    public PairingKeySerParameter getPublicKey() {
        return publicKey;
    }

    public BigInteger getReceiverHash() {
        return receiverHash;
    }

    public NodeDTO getSenderDTO() {
        return senderDTO;
    }

    public byte[] getGroupAtributesDTOBytesEncrypted() {
        return groupAtributesDTOBytesEncrypted;
    }

    public void setGroupAtributesDTOBytesEncrypted(byte[] groupAtributesDTOBytesEncrypted) {
        this.groupAtributesDTOBytesEncrypted = groupAtributesDTOBytesEncrypted;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }
    
}
