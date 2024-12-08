package psd.group4.events;

import java.math.BigInteger;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import psd.group4.dtos.NodeDTO;
import psd.group4.message.UserMessage;

public class NodeSendGroupMessageEvent extends NodeEvent {
    private byte[] messageEncryp;
    private NodeDTO senderDTO;
    private byte[] messageHash;

    private String groupName;
    private BigInteger groupNameHash;

    PairingKeySerParameter publicKey;
    PairingKeySerParameter secretKey;

    public NodeSendGroupMessageEvent(UserMessage msg) {
        super(msg);
        this.messageEncryp = msg.getMessageEncrypGroup();
        this.senderDTO = msg.getSenderDTO();
        this.messageHash = msg.getMessageHash();
        this.groupName = msg.getGroupName();
        this.groupNameHash = msg.getGroupNameHash();
    }

    public byte[] getMessageEncryp() {
        return this.messageEncryp;
    }

    public NodeDTO getSenderDTO() {
        return this.senderDTO;
    }

    public byte[] getMessageHash() {
        return this.messageHash;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public PairingKeySerParameter getPublicKey() {
        return publicKey;
    }

    public PairingKeySerParameter getSecretKey() {
        return secretKey;
    }

    public BigInteger getGroupNameHash() {
        return this.groupNameHash;
    }

}
