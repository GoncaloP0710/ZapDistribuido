package psd.group4.dtos;

import java.io.Serializable;
import java.math.BigInteger;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import it.unisa.dia.gas.jpbc.PairingParameters;

public class GroupAtributesDTO implements Serializable { // Facilitates the serialization of the object to be able to be encrypted

    // Critical data
    private int[][] accessPolicy;
    private String[] rhos;
    private PairingParameters pairingParameters;
    private String groupName;
    private String[] attributes;
    private PairingKeySerParameter masterKey;
    private BigInteger groupNameHash;

    public GroupAtributesDTO(int[][] accessPolicy, String[] rhos, PairingParameters pairingParameters, String groupName,
            String[] attributes, PairingKeySerParameter masterKey, BigInteger groupNameHash) {
        this.accessPolicy = accessPolicy;
        this.rhos = rhos;
        this.pairingParameters = pairingParameters;
        this.groupName = groupName;
        this.attributes = attributes;
        this.masterKey = masterKey;
        this.groupNameHash = groupNameHash;
    }

    public int[][] getAccessPolicy() {
        return accessPolicy;
    }

    public String[] getRhos() {
        return rhos;
    }

    public PairingParameters getPairingParameters() {
        return pairingParameters;
    }

    public String getGroupName() {
        return groupName;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public PairingKeySerParameter getMasterKey() {
        return masterKey;
    }

    public void setPairingParameters(PairingParameters pairingParameters) {
        this.pairingParameters = pairingParameters;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setAttributes(String[] attributes) {
        this.attributes = attributes;
    }

    public void setMasterKey(PairingKeySerParameter masterKey) {
        this.masterKey = masterKey;
    }

    public void setAccessPolicy(int[][] accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public void setRhos(String[] rhos) {
        this.rhos = rhos;
    }

    public BigInteger getGroupNameHash() {
        return groupNameHash;
    }

}
