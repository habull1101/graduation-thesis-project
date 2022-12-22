package com.example.vpnapp;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class UnitPacket implements Serializable {
    private static final long serialVersionUID = 42L;

    private InetSocketAddress src;
    private InetSocketAddress des;
    private byte[] content;

    public UnitPacket(InetSocketAddress src, InetSocketAddress des, byte[] content) {
        this.src = src;
        this.des = des;
        this.content = content;
    }

    public InetSocketAddress getSrc() {
        return src;
    }

    public void setSrc(InetSocketAddress src) {
        this.src = src;
    }

    public InetSocketAddress getDes() {
        return des;
    }

    public void setDes(InetSocketAddress des) {
        this.des = des;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
