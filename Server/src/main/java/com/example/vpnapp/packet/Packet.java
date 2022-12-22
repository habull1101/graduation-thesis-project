package com.example.vpnapp.packet;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Packet implements Serializable {
    static final long serialVersionUID = 1;
        
    public static final int IP4_HEADER_SIZE = 20;
    public static final int TCP_HEADER_SIZE = 20;
    public static final int UDP_HEADER_SIZE = 8;

    public IPv4Header ipv4Header;
    public TCPHeader tcpHeader;
    public UDPHeader udpHeader;
    public ByteBuffer backingBuffer;

    public boolean isTCP;
    public boolean isUDP;

    public Packet() {

    }

    public Packet(ByteBuffer buffer) throws UnknownHostException {
        this.ipv4Header = new IPv4Header(buffer);

        if (this.ipv4Header.protocolNum == 6) {
            this.tcpHeader = new TCPHeader(buffer);
            this.isTCP = true;
        }
        else if (ipv4Header.protocolNum == 17) {
            this.udpHeader = new UDPHeader(buffer);
            this.isUDP = true;
        }

        this.backingBuffer = buffer;
    }

    public boolean isTCP() {
        return isTCP;
    }

    public boolean isUDP() {
        return isUDP;
    }

    private void fillHeader(ByteBuffer buffer) {
        ipv4Header.fillHeader(buffer);
        if (isUDP)
            udpHeader.fillHeader(buffer);
        else if (isTCP)
            tcpHeader.fillHeader(buffer);
    }

    public void updateTCPBuffer(ByteBuffer buffer, byte flags, long sequenceNum, long ackNum, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        tcpHeader.flags = flags;
        backingBuffer.put(IP4_HEADER_SIZE + 13, flags);

        tcpHeader.sequenceNum = sequenceNum;
        backingBuffer.putInt(IP4_HEADER_SIZE + 4, (int) sequenceNum);

        tcpHeader.acknowledgementNum = ackNum;
        backingBuffer.putInt(IP4_HEADER_SIZE + 8, (int) ackNum);

        // Reset header size, since we don't need options
        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
        tcpHeader.offsetAndReserved = dataOffset;
        backingBuffer.put(IP4_HEADER_SIZE + 12, dataOffset);

        updateTCPChecksum(payloadSize);

        int ip4TotalLength = IP4_HEADER_SIZE + TCP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ipv4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    public void updateUDPBuffer(ByteBuffer buffer, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(IP4_HEADER_SIZE + 4, (short) udpTotalLength);
        udpHeader.length = udpTotalLength;

        // Disable UDP checksum validation
        backingBuffer.putShort(IP4_HEADER_SIZE + 6, (short) 0);
        udpHeader.checksum = 0;

        int ip4TotalLength = IP4_HEADER_SIZE + udpTotalLength;
        backingBuffer.putShort(2, (short) ip4TotalLength);
        ipv4Header.totalLength = ip4TotalLength;

        updateIP4Checksum();
    }

    private void updateIP4Checksum() {
        ByteBuffer buffer = backingBuffer.duplicate();
        buffer.position(0);

        // Clear previous checksum
        buffer.putShort(10, (short) 0);

        int ipLength = ipv4Header.headerLength;
        int sum = 0;
        while (ipLength > 0) {
            sum += getUnsignedShort(buffer.getShort());
            ipLength -= 2;
        }
        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;

        ipv4Header.headerChecksum = sum;
        backingBuffer.putShort(10, (short) sum);
    }

    private void updateTCPChecksum(int payloadSize) {
        int sum = 0;
        int tcpLength = TCP_HEADER_SIZE + payloadSize;

        // Calculate pseudo-header checksum 12 bytes
        // pseudo-header includes IP source, IP dest, 8 bit fixed, protocol number, TCP length
        ByteBuffer buffer = ByteBuffer.wrap(ipv4Header.sourceAddress.getAddress());
        sum = getUnsignedShort(buffer.getShort()) + getUnsignedShort(buffer.getShort());
        buffer = ByteBuffer.wrap(ipv4Header.destinationAddress.getAddress());
        sum += getUnsignedShort(buffer.getShort()) + getUnsignedShort(buffer.getShort());
        sum += 6 + tcpLength; // 6 is protocol number of TCP

        // Clear previous checksum
        buffer = backingBuffer.duplicate();
        buffer.putShort(IP4_HEADER_SIZE + 16, (short) 0);

        // Calculate TCP checksum
        // TCP checksum includes: Pseudo IP Header, TCP Header, TCP Body
        buffer.position(IP4_HEADER_SIZE);

        while (tcpLength > 1) {
            sum += getUnsignedShort(buffer.getShort());
            tcpLength -= 2;
        }

        if (tcpLength > 0)
            sum += getUnsignedByte(buffer.get()) << 8;

        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;

        tcpHeader.checksum = sum;
        backingBuffer.putShort(IP4_HEADER_SIZE + 16, (short) sum);
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