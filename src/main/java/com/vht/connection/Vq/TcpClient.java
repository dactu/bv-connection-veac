package com.vht.connection.Vq;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class TcpClient extends Thread{

    public String host;
    public int port;
    private AtomicBoolean isReceivingData;
    private AtomicBoolean stopRunning;
    private ChannelFuture channelFuture;
    BlockingQueue<String> queue;
    BlockingQueue<String> queue1;
    TcpClientHandler tcpClientHandler;
    public TcpClient(String host, int port){
        stopRunning = new AtomicBoolean(false);
        isReceivingData = new AtomicBoolean(false);
        this.host = host;
        this.port = port;
        this.queue = queue;
        this.queue1 = queue1;

    }
    @Override
    public void run(){
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.remoteAddress(new InetSocketAddress(host, port));
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                tcpClientHandler= new TcpClientHandler(isReceivingData);
                ch.pipeline().addLast(tcpClientHandler);
            }
        });

        // Start the client
        while (!stopRunning.get()) {
            try {
                if(Thread.currentThread().isInterrupted()){
                    Thread.interrupted();
                }
                channelFuture = bootstrap.connect().sync();
                log.info("\tConnected to %s\n", channelFuture.channel().remoteAddress().toString());
                // add ChannelFutureListener for detecting connection lost
                channelFuture.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                    if (channelFuture != null) {
                        try {
                            log.info("\tDetect connection lost {}\n", channelFuture.channel().remoteAddress());

                            channelFuture.channel().close().sync();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                log.info(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        try {
            if(Thread.currentThread().isInterrupted()){
                Thread.interrupted();
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        workerGroup.shutdownGracefully();
    }

    public void sendMessage(ByteBuf message){
        if(tcpClientHandler!=null) {
            tcpClientHandler.sendMessage(message);
        }
    }

}

