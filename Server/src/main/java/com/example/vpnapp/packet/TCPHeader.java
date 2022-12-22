package com.example.vpnapp.packet;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class TCPHeader implements Serializable {
    public static final int FIN = 0x01; //1
    public static final int SYN = 0x02; //2
    public static final int RST = 0x04; //4
    public static final int ACK = 0x10; //16

    // info TCP packet
    public int sourPort;
    public int destPort;
    public long sequenceNum;
    public long acknowledgementNum;
    public byte offsetAndReserved;
    public int headerLength;
    public byte flags;
    public int window;
    public int checksum;
    public int urgentPointer;
    public byte[] options;

    public TCPHeader() {
    }

    public TCPHeader(ByteBuffer buffer) {
        this.sourPort = getUnsignedShort(buffer.getShort());
        this.destPort = getUnsignedShort(buffer.getShort());

        this.sequenceNum = getUnsignedInt(buffer.getInt());
        this.acknowledgementNum = getUnsignedInt(buffer.getInt());

        this.offsetAndReserved = buffer.get();
        this.headerLength = (this.offsetAndReserved & 0xF0) >> 2;
        this.flags = buffer.get();
        this.window = getUnsignedShort(buffer.getShort());
        this.checksum = getUnsignedShort(buffer.getShort());
        this.urgentPointer = getUnsignedShort(buffer.getShort());

        int optionsLength = this.headerLength - 20;
        if (optionsLength > 0) {
            options = new byte[optionsLength];
            buffer.get(options, 0, optionsLength);
        }
    }

    public boolean isFIN() {
        return (flags & FIN) == FIN;
    }

    public boolean isSYN() {
        return (flags & SYN) == SYN;
    }

    public boolean isRST() {
        return (flags & RST) == RST;
    }

    public boolean isACK() {
        return (flags & ACK) == ACK;
    }

    public void fillHeader(ByteBuffer buffer) {
        buffer.putShort((short) sourPort);
        buffer.putShort((short) destPort);

        buffer.putInt((int) sequenceNum);
        buffer.putInt((int) acknowledgementNum);

        buffer.put(offsetAndReserved);
        buffer.put(flags);
        buffer.putShort((short) window);
        buffer.putShort((short) checksum);
        buffer.putShort((short) urgentPointer);
    }

    public static short getUnsignedByte(byte value) {
        return (short) (value & 0xFF);
    }

    public static int getUnsignedShort(short value) {
        return value & 0xFFFF;
    }

    public static long getUnsignedInt(int value) {
        return value & 0xFFFFFFFFL;
    }
}
