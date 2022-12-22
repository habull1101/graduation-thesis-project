package com.example.vpnapp;

import static com.example.vpnapp.VpnServices.STATUS_CONNECTED;
import static com.example.vpnapp.VpnServices.STATUS_DISCONNECTED;

import android.net.VpnService;
import android.util.Log;

import com.example.vpnapp.packet.BuildPacket;
import com.example.vpnapp.packet.Packet;
import com.example.vpnapp.packet.TCBStatus;
import com.example.vpnapp.packet.TCPHeader;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class HandleTcp implements Runnable {
    private static final String TAG = HandleTcp.class.getSimpleName();
    private static final int TOTAL_HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE;

    BlockingQueue<Packet> deviceToNetworkTCPQueue;
    BlockingQueue<ByteBuffer> networkToDeviceQueue;
    private VpnService vpnService;

    private final String ipServer = "192.168.1.105";
    private final int portServer = 9000;
    Channel channelNetty;

    static ConcurrentHashMap<String, TcpTunnel> tunnels = new ConcurrentHashMap();

    public HandleTcp(BlockingQueue<Packet> deviceToNetworkTCPQueue, BlockingQueue<ByteBuffer> networkToDeviceQueue,
                     VpnService vpnService) {
        this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.vpnService = vpnService;

        initNetty();
    }

    private void initNetty() {
        Thread nettyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    KeyStore keyStore = KeyStore.getInstance("BKS");
                    keyStore.load(MainActivity.context.getResources().openRawResource(R.raw.cacerts), "changeit".toCharArray());
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
                    trustManagerFactory.init(keyStore);

                    SslContext sslCtx = SslContextBuilder.forClient()
                            .trustManager(trustManagerFactory)
                            .build();

                    Bootstrap b = new Bootstrap();
                    b.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
                            @Override
                            public void initChannel(io.netty.channel.socket.SocketChannel ch) throws Exception {
                                ChannelPipeline p = ch.pipeline();

                                p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
                                p.addLast(new ProtobufVarint32FrameDecoder());
                                p.addLast(new ProtobufDecoder(Test.packet.getDefaultInstance()));
                                p.addLast(new ProtobufVarint32LengthFieldPrepender());
                                p.addLast(new ProtobufEncoder());
                                p.addLast(new ClientHandler());
                            }
                        });

                    // Start the client.
                    ChannelFuture f = b.connect(ipServer, portServer).sync();
                    Log.i("Test", "Client connected");

                    Channel channel = f.sync().channel();

                    channelNetty = channel;

                    VpnServices.statusConnect.postValue(STATUS_CONNECTED);

                    // Wait until the connection is closed.
                    f.channel().closeFuture().sync();
                } catch (Exception e) {
                    e.printStackTrace();
                    VpnServices.statusConnect.postValue(STATUS_DISCONNECTED);
                } finally {
                    // Shut down the event loop to terminate all threads.
                    group.shutdownGracefully();
                }
            }
        });
        nettyThread.start();
    }

    static class TcpTunnel {
        public long mySequenceNum = 0;
        public long theirSequenceNum = 0;
        public long myAcknowledgementNum = 0;
        public long theirAcknowledgementNum = 0;

        public TCBStatus tcbStatus = TCBStatus.SYN_SENT;
        public InetSocketAddress sourceAddress;
        public InetSocketAddress destinationAddress;
        public SocketChannel destSocket;
        private VpnService vpnService;
        BlockingQueue<ByteBuffer> networkToDeviceQueue;
        public BlockingQueue<Packet> tunnelInputQueue = new ArrayBlockingQueue<Packet>(1024);

        public int packId = 1;
    }

    private TcpTunnel initTunnel(Packet packet) {
        TcpTunnel tunnel = new TcpTunnel();
        tunnel.sourceAddress = new InetSocketAddress(packet.ipv4Header.sourceAddress, packet.tcpHeader.sourPort);
        tunnel.destinationAddress = new InetSocketAddress(packet.ipv4Header.destinationAddress, packet.tcpHeader.destPort);
        tunnel.vpnService = vpnService;
        tunnel.networkToDeviceQueue = networkToDeviceQueue;

        Thread t = new Thread(new UpStreamWorker(tunnel, channelNetty));
        t.start();

        return tunnel;
    }

    static void sendTcpPack(TcpTunnel tunnel, byte flag, byte[] data) {
        int dataLen = 0;
        if (data != null)
            dataLen = data.length;

        Packet packet = BuildPacket.buildTcpPacket(tunnel.destinationAddress, tunnel.sourceAddress, flag,
                tunnel.myAcknowledgementNum, tunnel.mySequenceNum, tunnel.packId);
        tunnel.packId += 1;

        ByteBuffer byteBuffer = ByteBuffer.allocate(16384);
        byteBuffer.position(TOTAL_HEADER_SIZE);
        if (data != null) {
            if (byteBuffer.remaining() < data.length) {
                System.currentTimeMillis();
            }

            byteBuffer.put(data);
        }

        packet.updateTCPBuffer(byteBuffer, flag, tunnel.mySequenceNum, tunnel.myAcknowledgementNum, dataLen);
        byteBuffer.position(TOTAL_HEADER_SIZE + dataLen);

        tunnel.networkToDeviceQueue.offer(byteBuffer);

        if ((flag & (byte) TCPHeader.SYN) != 0) {
            tunnel.mySequenceNum += 1;
        }
        if ((flag & (byte) TCPHeader.ACK) != 0) {
            tunnel.mySequenceNum += dataLen;
        }
    }

    private static class UpStreamWorker implements Runnable {
        TcpTunnel tunnel;
        int synCount = 0;
        private Channel channelNetty;

        public UpStreamWorker(TcpTunnel tunnel, Channel channelNetty) {
            this.tunnel = tunnel;
            this.channelNetty = channelNetty;
        }

        private void startDownStream() {
            Thread t = new Thread(new DownStreamWorker(tunnel));
            t.start();
        }

        private void writeToRemote(ByteBuffer buffer) throws IOException {
//            tunnel.destSocket.write(buffer);

            byte[] byteData = new byte[buffer.remaining()];
            buffer.get(byteData);

            Test.packet packet = Test.packet.newBuilder()
                                    .setId(tunnel.destinationAddress.getAddress().toString().split("/")[1] + ":" + tunnel.destinationAddress.getPort()
                                        + ":" + tunnel.sourceAddress.getPort())
                                    .setContent(ByteString.copyFrom(byteData))
                                    .build();

            channelNetty.writeAndFlush(packet);
            Log.i("Test", "Send to Server: " + packet.getContent().toByteArray().length + " byte");
        }

        private void connectRemote() {
            try {
                SocketChannel remote = SocketChannel.open();
                remote.socket().connect(tunnel.destinationAddress, 5000);

                tunnel.destSocket = remote;

                startDownStream();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        private void loop() {
            while (true) {
                try {
                    Packet packet = tunnel.tunnelInputQueue.take();

                    synchronized (tunnel) {
                        TCPHeader tcpHeader = packet.tcpHeader;

                        if (tcpHeader.isSYN())
                            handleSyn(packet);
                        if (tcpHeader.isACK())
                            handleAck(packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        private void handleSyn(Packet packet) {
            if (tunnel.tcbStatus == TCBStatus.SYN_SENT)
                tunnel.tcbStatus = TCBStatus.SYN_RECEIVED;

            TCPHeader tcpHeader = packet.tcpHeader;
            if (synCount == 0) {
                tunnel.mySequenceNum = 1;
                tunnel.theirSequenceNum = tcpHeader.sequenceNum;
                tunnel.myAcknowledgementNum = tcpHeader.sequenceNum + 1;
                tunnel.theirAcknowledgementNum = tcpHeader.acknowledgementNum;
                sendTcpPack(tunnel, (byte) (TCPHeader.SYN | TCPHeader.ACK), null);
            }
            else
                tunnel.myAcknowledgementNum = tcpHeader.sequenceNum + 1;

            synCount++;
        }

        private void handleAck(Packet packet) throws IOException {
            Log.i("Test", "handleAck");
            if (tunnel.tcbStatus == TCBStatus.SYN_RECEIVED)
                tunnel.tcbStatus = TCBStatus.ESTABLISHED;

            TCPHeader tcpHeader = packet.tcpHeader;
            int payloadSize = packet.backingBuffer.remaining();
            if (payloadSize == 0)
                return;

            long newAck = tcpHeader.sequenceNum + payloadSize;
            if (newAck <= tunnel.myAcknowledgementNum)
                return;

            tunnel.myAcknowledgementNum = tcpHeader.sequenceNum;
            tunnel.theirAcknowledgementNum = tcpHeader.acknowledgementNum;
            tunnel.myAcknowledgementNum += payloadSize;
            writeToRemote(packet.backingBuffer);

            sendTcpPack(tunnel, (byte) TCPHeader.ACK, null);

            System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                connectRemote();
                loop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class DownStreamWorker implements Runnable {
        TcpTunnel tunnel;

        public DownStreamWorker(TcpTunnel tunnel) {
            this.tunnel = tunnel;
        }

        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);

            try {
                while (true) {
                    buffer.clear();
                    if (tunnel.destSocket == null) {
                        throw new Exception("tunnel maybe closed");
                    }
                    int n = tunnel.destSocket.read(buffer);

                    synchronized (tunnel) {
                        if (n == -1) {
                            break;
                        } else {
//                            Log.i("Test", "Receive from socket");

                            if (tunnel.tcbStatus != TCBStatus.CLOSE_WAIT) {
                                buffer.flip();
                                byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);
                                sendTcpPack(tunnel, (byte) (TCPHeader.ACK), data);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, String.format("error", e.getMessage()));
            }
        }
    }

    // Lấy từng packet trong deviceToNetworkTCP ra rồi tạo tunnel.class theo info packet đó
    @Override
    public void run() {
        while (true) {
            try {
                Packet currentPacket = deviceToNetworkTCPQueue.take();
                InetAddress destinationAddress = currentPacket.ipv4Header.destinationAddress;
                TCPHeader tcpHeader = currentPacket.tcpHeader;
                int destinationPort = tcpHeader.destPort;
                int sourcePort = tcpHeader.sourPort;
                String ipAndPort = destinationAddress.getHostAddress() + ":" +
                        destinationPort + ":" + sourcePort;

                if (!tunnels.containsKey(ipAndPort)) {
                    TcpTunnel tcpTunnel = initTunnel(currentPacket);
                    tunnels.put(ipAndPort, tcpTunnel);
                }
                TcpTunnel tcpTunnel = tunnels.get(ipAndPort);
                tcpTunnel.tunnelInputQueue.offer(currentPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}