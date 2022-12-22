package com.example.vpnapp;

import android.os.Build;
import android.util.Log;
//
//import com.example.vpnapp.packet.SocketObject;
//import com.example.vpnapp.packet.TCBStatus;
//import com.example.vpnapp.packet.TCPHeader;
//
//import java.nio.ByteBuffer;
//
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//
//public class ClientHandler extends ChannelInboundHandlerAdapter {
//    // duoc goi moi khi co client moi
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        Log.i("Test", "Joined");
//    }
//
//    // duoc goi khi client nhan duoc data
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
////            Log.i("Test", "Client received byte[]");
//
////            byte[] received  = ((byte[])object);
////            ByteBuffer buffer = ByteBuffer.allocate(16384);
////            for (byte i: received)
////                buffer.put(i);
////            buffer.flip();
////            networkToDeviceQueue.offer(buffer);
//
////        SocketObject socketObject = (SocketObject) object;
////
////        Log.i("Test", "============= Client received =============");
////        Log.i("Test", "Receive from server: " + socketObject.getData().length);
////        for (byte i: socketObject.getData())
////            Log.i("Test", String.valueOf(i));
////
////        byte[] received  = socketObject.getData();
////        ByteBuffer buffer = ByteBuffer.allocate(16384);
////        for (byte i: received)
////            buffer.put(i);
////        buffer.flip();
//
//        Log.i("Test", "Received from Server: ")
////
////        String ipAndPort = socketObject.getDestIp() + ":" +
////                socketObject.getDestPort() + ":" + socketObject.getSourcePort();
////        HandleTcp.TcpTunnel tunnel = HandleTcp.tunnels.get(ipAndPort);
////        synchronized (tunnel) {
////            if (tunnel.tcbStatus != TCBStatus.CLOSE_WAIT) {
////                buffer.flip();
////                byte[] data = new byte[buffer.remaining()];
////                buffer.get(data);
////                HandleTcp.sendTcpPack(tunnel, (byte) (TCPHeader.ACK), data);
////            }
////        }
//    }
//
//    // dong connect trong truong hop co ngoai le
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();
//        ctx.close();
//    }
//}

//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.net.Socket;
//
//public class ServerHandler extends ChannelInboundHandlerAdapter{
//
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("New client joined - " + ctx);
//    }
//
//    // duoc goi khi server nhan duoc data
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {
//        System.out.println("Received: " + object.toString());
//
////        Thread t1 = new Thread(new Runnable() {
////            @Override
////            public void run() {
////                Socket socket;
////                try {
////                    socket = new Socket("192.168.136.128", 12345);
////
////                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
////                    out.write();
////                } catch (IOException ex) {
////                    ex.printStackTrace();
////                }
////            }
////        });
////        t1.start();
//    }
//
//    // dong connect trong truong hop co ngoai le
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();
//        ctx.close();
//    }
//}

import androidx.annotation.RequiresApi;

import com.example.vpnapp.packet.TCBStatus;
import com.example.vpnapp.packet.TCPHeader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@ChannelHandler.Sharable
public class ClientHandler extends SimpleChannelInboundHandler<Test.packet>{

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Log.i("Test", "New client joined - " + ctx);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Test.packet packet) throws IOException {
        String key = packet.getId();
        byte[] content = packet.getContent().toByteArray();

        HandleTcp.TcpTunnel tunnel = HandleTcp.tunnels.get(key);
        synchronized (tunnel) {
            if (tunnel.tcbStatus != TCBStatus.CLOSE_WAIT) {
                HandleTcp.sendTcpPack(tunnel, (byte) (TCPHeader.ACK), content);
            }
        }

        Log.i("Test", "Received from Server: " + content.length + " byte --------- ("
                + key + ")");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}