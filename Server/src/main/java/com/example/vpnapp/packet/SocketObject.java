package com.example.vpnapp.packet;

import java.io.Serializable;

public class SocketObject implements Serializable {
    private String destIp;
    private int destPort;
    private int sourcePort;
    private byte[] data;

    public SocketObject(String destIp, int destPort, int sourcePort, byte[] data) {
        this.destIp = destIp;
        this.destPort = destPort;
        this.sourcePort = sourcePort;
        this.data = data;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }
}
