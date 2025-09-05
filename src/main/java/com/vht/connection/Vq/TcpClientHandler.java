package com.vht.connection.Vq;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vht.connection.Vq.Manager.dataRawQueue;


@Log4j2
public class TcpClientHandler extends ChannelInboundHandlerAdapter {

//    private DataCapturerGrpcClient dataCapturerClient;
    private ChannelHandlerContext ctx;

    private ConcurrentLinkedQueue<String> rawDataNMEAQueue = new ConcurrentLinkedQueue<>();
    public TcpClientHandler(AtomicBoolean isReceivingData) {
        super();
        this.radarId = radarId;
        this.connectionId = connectionId;
        this.amountOfBytesReceived = amountOfBytesReceived;

        this.isReceivingData = isReceivingData;
        this.dataCaptured = dataCaptured;

        this.outputOfTransporter = outputOfTransporter;
//        dataCapturerClient = DataCapturerGrpcClient.getInstance();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx=ctx;
//        ctx.writeAndFlush(Unpooled.copiedBuffer("abcdef", StandardCharsets.UTF_8));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
//        log.info("Receive a tcp message %s",ctx.channel().remoteAddress().toString());
        isReceivingData.set(true);
        ByteBuf m = (ByteBuf) msg; // (1)
        ByteBuf copyVersion = m.copy();
        ByteString byteString = ByteString.copyFrom(ByteBufUtil.getBytes(copyVersion));
        String rawData = m.toString(StandardCharsets.UTF_8);
        DataRaw dataRaw = new DataRaw("vb1m", rawData);
        if(dataRawQueue.size()>100) {
            dataRawQueue.poll();
        }
        if (!dataRawQueue.offer(dataRaw)) {
            dataRawQueue.clear();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    public void sendMessage(ByteBuf message){
        ctx.writeAndFlush(message);
    }


    AtomicInteger amountOfBytesReceived;
    private int connectionId;
    private int radarId;
    private LinkedBlockingQueue<ByteBuf> outputOfTransporter;
    private LinkedBlockingQueue<ByteBuf> dataCaptured;
    public AtomicBoolean isReceivingData;
}