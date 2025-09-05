package com.vht.connection.TCPConnectionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class TCPServer extends Thread{
    private LinkedBlockingQueue<ByteBuf> outOfTCPServer= new LinkedBlockingQueue<>(1000);
    private int tcpPort;
    private AtomicBoolean stopRunning;
    public void stopRunning() {
        stopRunning.set(true);
    }

    public ByteBuf pollQueue(){
        if(!outOfTCPServer.isEmpty()){
            return outOfTCPServer.poll();
        }else{
            return null;
        }
    }

    public TCPServer(int tcpPort){
        this.tcpPort = tcpPort;
        stopRunning = new AtomicBoolean(false);
    }

    @Override
    public void run(){
        NioEventLoopGroup workerGroup= new NioEventLoopGroup();
        NioEventLoopGroup bossGroup= new NioEventLoopGroup();

        try{
                ServerBootstrap server= new ServerBootstrap();
                server.group(workerGroup,bossGroup).channel(NioServerSocketChannel.class)
                                .option(ChannelOption.SO_BACKLOG, 100)
                                        .handler(new LoggingHandler(LogLevel.INFO));

                server.childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel){
                        socketChannel.pipeline().addLast(new ByteArrayEncoder());
                        socketChannel.pipeline().addLast(new TCPServerHandle(outOfTCPServer));

                    }
                }
                );
                //start server
                ChannelFuture channelFuture= null;
                try{
                    channelFuture= server.bind(tcpPort).sync();
                    log.info("server bind successfull port: {}",tcpPort);
                }catch(InterruptedException e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }

                try{
                    if(channelFuture!=null) {
                        channelFuture.channel().closeFuture().sync();
                    } else{
                        log.info("[TCPServer] channelFuture is null, can't close future channel");
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
//            }
        }finally{
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
//    public void sendMessage(byte[] message){
//        handle.sendMessage(message);
//    }
//
//    public void sendMessage(ByteBuf message){
//        handle.sendMessage(message);
//    }
}

