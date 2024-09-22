package dtos;

import java.math.BigInteger;

public class NodeDTO {

    private String ip;
    private int port;
    private BigInteger hash;

    protected NodeDTO() {}

    public NodeDTO(String ip, int port, BigInteger hash) {
        this.ip = ip;
        this.port = port;
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
    
}
