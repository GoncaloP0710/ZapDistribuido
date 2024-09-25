package dtos;

import java.io.Serializable;
import java.math.BigInteger;

public class NodeDTO implements Serializable {

    String name;
    private String ip;
    private int port;
    private BigInteger hash;

    public NodeDTO(String name, String ip, int port, BigInteger hash) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.hash = hash;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
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
    
}
