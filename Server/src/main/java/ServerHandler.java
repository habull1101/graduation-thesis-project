import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<Test.packet>{
    ConcurrentHashMap<String, Socket> mapSocket = new ConcurrentHashMap();
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New client joined - " + ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Test.packet packet) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        String key = packet.getId();
        byte[] content = packet.getContent().toByteArray();
        
        System.out.println("Received from Android: " + content.length + " byte/" + key);
               
        if (!mapSocket.containsKey(key)) {
            String[] listId = key.split(":");
                        
            Socket socket = new Socket(listId[0], Integer.parseInt(listId[1]));
            mapSocket.put(key, socket);
            System.out.println("Open new socket");
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        while (true) {
                            byte[] received = new byte[1024];
                            
                            int n = in.read(received);
                            if (n == -1) {
                                break;
                            }
                            else {
                                byte[] receiveTmp = Arrays.copyOfRange(received, 0, n);
                                
                                Test.packet newPacket = Test.packet.newBuilder()
                                                        .setId(packet.getId())
                                                        .setContent(ByteString.copyFrom(receiveTmp))
                                                        .build();
                                ctx.writeAndFlush(newPacket);
                                System.out.println("Received from Server app / Send to Android: " + newPacket.getContent().toByteArray().length + " byte");
                            }
                        }
                    }
                    catch (IOException ex) {
                        Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start(); 
        }
        
        Socket socket = mapSocket.get(key);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.write(content);
        out.flush();
        
        System.out.println("Send to Server app: " + content.length + " byte ------ (" 
                + key + ")");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}