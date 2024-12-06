package psd.group4.events;

import java.math.BigInteger;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import it.unisa.dia.gas.jpbc.PairingParameters;
import psd.group4.message.ChordInternalMessage;

public class AddUserToGroupEvent extends NodeEvent {
    private final PairingKeySerParameter publicKey;
    private final int[][] accessPolicy;
    private final String[] rhos;
    private PairingParameters pairingParameters;
    private BigInteger receiverHash;
    private String groupName;
    private String[] attributes;
    private PairingKeySerParameter masterKey;

    public AddUserToGroupEvent(ChordInternalMessage msg) {
        super(msg);
        this.publicKey = msg.getPublicKey();
        this.accessPolicy = msg.getAccessPolicy();
        this.rhos = msg.getRhos();
        this.receiverHash = msg.getReceiverHash();
        this.groupName = msg.getGroupName();
        this.pairingParameters = msg.getPairing();
        this.attributes = msg.getAttributes();
        this.masterKey = msg.getMasterKey();
    }

    public PairingKeySerParameter getPublicKey() {
        return publicKey;
    }

    public int[][] getAccessPolicy() {
        return accessPolicy;
    }

    public String[] getRhos() {
        return rhos;
    }

    public BigInteger getReceiverHash() {
        return receiverHash;
    }

    public String getGroupName() {
        return groupName;
    }

    public PairingParameters getPairingParameters() {
        return pairingParameters;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public PairingKeySerParameter getMasterKey() {
        return masterKey;
    }
    
}
