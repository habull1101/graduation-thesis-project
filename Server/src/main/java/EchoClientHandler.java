import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@ChannelHandler.Sharable
public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf>{
    Socket socket;
    DataOutputStream out;
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("new client");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.136.128", 12345);     
                    out = new DataOutputStream(socket.getOutputStream());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }            }
        }).start();
       
//        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!", CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws IOException {
//        ctx.writeAndFlush(Unpooled.copiedBuffer("Client received: " + in.toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8));
        System.out.println(Unpooled.copiedBuffer("Client received: " + in.toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8));
        out.writeUTF("hehe");
        out.flush();

//        Thread t1 = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Socket socket;
//                try {
//                    socket = new Socket("192.168.136.128", 12345);
//                    
//                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    out.writeUTF("hehehlll");
//                    out.flush();                    
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }); 
//        t1.start();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}