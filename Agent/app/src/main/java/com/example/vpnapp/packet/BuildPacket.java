package com.example.vpnapp.packet;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class BuildPacket implements Serializable {
    public static Packet buildTcpPacket(InetSocketAddress source, InetSocketAddress dest, byte flag,
                                        long ack, long seq, int ipId) {
        Packet packet = new Packet();
        packet.isTCP = true;
        packet.isUDP = false;

        IPv4Header ipv4Header = new IPv4Header();
        ipv4Header.version = 4;
        ipv4Header.IHL = 5;
        ipv4Header.destinationAddress = dest.getAddress();
        ipv4Header.headerChecksum = 0;
        ipv4Header.headerLength = 20;

        //int ipId=0;
        int ipFlag = 0x40;
        int ipOff = 0;

        ipv4Header.identificationAndFlagsAndFragmentOffset = ipId << 16 | ipFlag << 8 | ipOff;

        ipv4Header.options = 0;
        ipv4Header.protocolNum = 6;
        ipv4Header.sourceAddress = source.getAddress();
        ipv4Header.totalLength = 60;
        ipv4Header.typeOfService = 0;
        ipv4Header.TTL = 64;

        TCPHeader tcpHeaderTmp = new TCPHeader();
        tcpHeaderTmp.acknowledgementNum = ack;
        tcpHeaderTmp.checksum = 0;
        tcpHeaderTmp.offsetAndReserved = -96;
        tcpHeaderTmp.destPort = dest.getPort();
        tcpHeaderTmp.flags = flag;
        tcpHeaderTmp.headerLength = 40;
        tcpHeaderTmp.options = null;
        tcpHeaderTmp.sequenceNum = seq;
        tcpHeaderTmp.sourPort = source.getPort();
        tcpHeaderTmp.urgentPointer = 0;
        tcpHeaderTmp.window = 65535;

        ByteBuffer byteBuffer = ByteBuffer.allocate(16384);
        byteBuffer.flip();

        packet.ipv4Header = ipv4Header;
        packet.tcpHeader = tcpHeaderTmp;
        packet.backingBuffer = byteBuffer;

        return packet;
    }

    public Packet buildUdpPacket(InetSocketAddress source, InetSocketAddress dest, int ipId) {
        Packet packet = new Packet();
        packet.isTCP = false;
        packet.isUDP = true;

        IPv4Header ipv4Header = new IPv4Header();
        ipv4Header.version = 4;
        ipv4Header.IHL = 5;
        ipv4Header.destinationAddress = dest.getAddress();
        ipv4Header.headerChecksum = 0;
        ipv4Header.headerLength = 20;

        //int ipId=0;
        int ipFlag = 0x40;
        int ipOff = 0;

        ipv4Header.identificationAndFlagsAndFragmentOffset = ipId << 16 | ipFlag << 8 | ipOff;

        ipv4Header.options = 0;
        ipv4Header.protocolNum = 17;
        ipv4Header.sourceAddress = source.getAddress();
        ipv4Header.totalLength = 60;
        ipv4Header.typeOfService = 0;
        ipv4Header.TTL = 64;

        UDPHeader udpHeaderTmp = new UDPHeader();
        udpHeaderTmp.sourcePort = source.getPort();
        udpHeaderTmp.destinationPort = dest.getPort();
        udpHeaderTmp.length = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(16384);
        byteBuffer.flip();

        packet.ipv4Header = ipv4Header;
        packet.udpHeader = udpHeaderTmp;
        packet.backingBuffer = byteBuffer;

        return packet;
    }
}
