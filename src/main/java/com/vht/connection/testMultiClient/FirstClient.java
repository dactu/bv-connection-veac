package com.vht.connection.testMultiClient;

import com.vht.connection.BVManager;
import com.vht.connection.Objects.BVModule;
import connection.status.ConnectionStatus;
import lombok.extern.log4j.Log4j2;
import vea.api.Common;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Log4j2
public class FirstClient {
    private Socket socket;
    private String ip;
    private int port;
    private boolean connectionStatus = false;
    public FirstClient(){

    }

    public FirstClient(String host, int port){
        this.ip = host;
        this.port = port;
    }

    public void createSocket() {
        log.info("Creating socket of radar");
        if(!connectionStatus) {
            try {
                socket = SocketFactory.getDefault().createSocket();
                socket.connect(new InetSocketAddress(ip, port), 1000);
                socket.setSoTimeout(5000);
                connectionStatus = socket.isConnected();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void sendData() {
        Thread threadSendQDTData = new Thread(()-> {
            while (true){
                try {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    byte[] dataSendByteArr = StatusMsgToByteArray(cloneRadarInfo());
                    if(socket != null && !socket.isClosed() && connectionStatus) {
                        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                        dataOut.writeShort(0x0F0F);
                        dataOut.writeInt(dataSendByteArr.length);
                        dataOut.write(dataSendByteArr);
                    } else {
                        log.error("Socket is closed, unable to send data!");
                    }
                } catch (IOException e) {
                    connectionStatus = false;
                    log.error("DISCONNECTED BECAUSE sendData EXCEPTION");
                }
            }
        });
        threadSendQDTData.start();
    }

    public byte[] StatusMsgToByteArray(ConnectionStatus.CSSensorConnection inputProtoObj) throws IOException {
        byte[] sslPayload = inputProtoObj.toByteArray();
        byte[] sslLength = intToByteArray(sslPayload.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(sslLength);
        outputStream.write(sslPayload);
        return sslPayload;
    }

    public byte[] intToByteArray(int input) {
        byte[] a = new byte[4];
        a[0] = (byte)(input >> 24);
        a[1] = (byte)(input >> 16);
        a[2] = (byte)(input >> 8);
        a[3] = (byte)(input);
        return a;
    }

    public ConnectionStatus.CSSensorConnection cloneRadarInfo(){
        ConnectionStatus.CSSensorConnection.Builder dataSend = ConnectionStatus.CSSensorConnection.newBuilder();

        ConnectionStatus.RadarInfor.Builder radarStt = ConnectionStatus.RadarInfor.newBuilder();
        radarStt.setName("VRS");
        radarStt.setMode(ConnectionStatus.CSMode.MO_KTCN);
        radarStt.setOperator(ConnectionStatus.CSOperator.OP_SSCD);
        dataSend.addRadarInfor(radarStt.build());
        return dataSend.build();
    }
}
