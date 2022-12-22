package com.example.vpnapp.packet;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv4Header implements Serializable {
    public byte version;
    public byte IHL;
    public int headerLength;
    public short typeOfService;
    public int totalLength;
    public int identificationAndFlagsAndFragmentOffset;
    public short TTL;
    public short protocolNum; // 6: TCP, 17: UDP
    public int headerChecksum;

    public InetAddress sourceAddress;
    public InetAddress destinationAddress;

    public int options;

    public IPv4Header() {
    }

    public IPv4Header(ByteBuffer buffer) throws UnknownHostException {
        byte versionAndIHL = buffer.get();
        this.version = (byte) (versionAndIHL >> 4);
        this.IHL = (byte) (versionAndIHL & 0x0F); // do dai ipv4 packet tinh bang so tu 32 bit
        this.headerLength = this.IHL * 4; // doi do dai sang byte

        this.typeOfService = getUnsignedByte(buffer.get());
        this.totalLength = getUnsignedShort(buffer.getShort());

        this.identificationAndFlagsAndFragmentOffset = buffer.getInt();

        this.TTL = getUnsignedByte(buffer.get());
        this.protocolNum = getUnsignedByte(buffer.get());
        this.headerChecksum = getUnsignedShort(buffer.getShort());

        byte[] addressBytes = new byte[4];
        buffer.get(addressBytes, 0, 4);
        this.sourceAddress = InetAddress.getByAddress(addressBytes);

        buffer.get(addressBytes, 0, 4);
        this.destinationAddress = InetAddress.getByAddress(addressBytes);

        //this.options = buffer.getInt();
    }

    public void fillHeader(ByteBuffer buffer) {
        buffer.put((byte) (this.version << 4 | this.IHL));
        buffer.put((byte) this.typeOfService);
        buffer.putShort((short) this.totalLength);

        buffer.putInt(this.identificationAndFlagsAndFragmentOffset);

        buffer.put((byte) this.TTL);
        buffer.put((byte) this.protocolNum);
        buffer.putShort((short) this.headerChecksum);

        buffer.put(this.sourceAddress.getAddress());
        buffer.put(this.destinationAddress.getAddress());
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
