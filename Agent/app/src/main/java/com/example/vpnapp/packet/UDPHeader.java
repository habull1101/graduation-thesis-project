package com.example.vpnapp.packet;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class UDPHeader implements Serializable {
    public int sourcePort;
    public int destinationPort;

    public int length;
    public int checksum;

    public UDPHeader(){
    }

    public UDPHeader(ByteBuffer buffer) {
        this.sourcePort = getUnsignedShort(buffer.getShort());
        this.destinationPort = getUnsignedShort(buffer.getShort());

        this.length = getUnsignedShort(buffer.getShort());
        this.checksum = getUnsignedShort(buffer.getShort());
    }

    public void fillHeader(ByteBuffer buffer) {
        buffer.putShort((short) this.sourcePort);
        buffer.putShort((short) this.destinationPort);

        buffer.putShort((short) this.length);
        buffer.putShort((short) this.checksum);
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
