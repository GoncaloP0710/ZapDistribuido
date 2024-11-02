package psd.group4.dtos;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.PublicKey;

public class NodeDTO implements Serializable {

    private String username;
    private String ip;
    private int port;
    private Certificate cer;
    private BigInteger hash;

    public NodeDTO(String username, String ip, int port, BigInteger hash, Certificate cer) {
        this.username = username;
        this.ip = ip;
        this.port = port;
        this.cer = cer;
        this.hash = hash;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return this.ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public void setHash(BigInteger hash) {
        this.hash = hash;
    }

    public BigInteger getHash() {
        return this.hash;
    }

    public String getUsername() {
        return this.username;
    }

    public PublicKey getPubK() {
        return this.cer.getPublicKey();
    } 
    
    @Override
    public String toString() {
        return "Node[" +
                "ip = " + ip + ", " +
                "port = " + port + ", " +
                "username = " + username + ']';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NodeDTO)) {
            return false;
        }
        NodeDTO node = (NodeDTO) obj;
        return node.getIp().equals(ip) && node.getPort() == port && node.getUsername().equals(username);
    }
}
