package com.vht.connection.TCPConnectionHandler;

import com.vht.connection.BVManager;
import com.vht.connection.Objects.BVModule;
import com.vht.connection.ReadConfigFile;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import vea.api.Common;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EnableScheduling
@Log4j2
@Component
@Configuration
public class HandleTCPConnection {
    public ConcurrentLinkedQueue<Common.Message> queueBVFromTCP = new ConcurrentLinkedQueue<>();
    private SSLSocket socket;
    private String ip;
    private int port;
    DataInputStream dataIn;
    //directory to store capture file
    private final String DIRECTORY_TO_CAPTURE = "Capture/BV5Data";
    private final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH");

    public HandleTCPConnection() {}

    public HandleTCPConnection(String _ip, int _port){
        ip = _ip;
        port = _port;
    }

    public void createSSLSocket(String bv5Id) {
        log.info("Creating ssl socket of " + bv5Id);
        BVModule module = BVManager.getInstance().getBVModuleById(bv5Id);
        if(!module.connectionStatus) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }};
                SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(null, trustAllCerts, new java.security.SecureRandom());
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) context.getSocketFactory();

                try {
                    module.handleTCPConnection.socket = (SSLSocket) sslSocketFactory.createSocket();
                    socket.connect(new InetSocketAddress(ip, port), 1000);
                    socket.setSoTimeout(5000);
                    module.connectionStatus = socket.isConnected();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    log.error(bv5Id + " : "  + e.getMessage());
                }
            } catch (Exception e) {
                log.error(bv5Id + " : " + e.getMessage());
            }
        }
    }

    public void sendData(String bv5Id, Common.Message dataSend) {
        BVModule bvModule = BVManager.getInstance().getBVModuleById(bv5Id);
        try {
            byte[] dataSendByteArr = ComMsgToByteArray(dataSend);
            if(bvModule.handleTCPConnection.socket != null && !bvModule.handleTCPConnection.socket.isClosed() && bvModule.connectionStatus) {
                DataOutputStream dataOut = new DataOutputStream(bvModule.handleTCPConnection.socket.getOutputStream());
                dataOut.writeInt(dataSendByteArr.length);
                dataOut.write(dataSendByteArr);
//                log.info("Sending data to " + bv5Id);
            } else {
                log.error("Socket is closed, unable to send data!");
            }
        } catch (IOException e) {
            bvModule.connectionStatus = false;
            bvModule.userAuthenStatus = false;
            log.error("DISCONNECTED BECAUSE sendData EXCEPTION");
            log.error(e.getMessage());
        }
    }

    public void receiveData(String i) {
        Thread threadReadingData = new Thread(()-> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BVModule bvModule = BVManager.getInstance().getBVModuleById(i);

                //check if directory exist
                File directory = new File(DIRECTORY_TO_CAPTURE);
                if(!directory.exists()){
                    if(!directory.mkdirs()){
                        return;
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                String fileName = now.format(FILENAME_FORMATTER) + ".bin";
                File outputFile = new File(DIRECTORY_TO_CAPTURE, fileName);
                try {
                    if (socket != null && bvModule.connectionStatus) {
                        if(ReadConfigFile.enableCaptureData){
                            try(DataOutputStream outputStream = new DataOutputStream(Files.newOutputStream(outputFile.toPath()))){
                                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                                int dataLen = dataIn.readInt();
                                outputStream.writeInt(dataLen);
                                if (dataLen > 0 && dataLen < 5 * 1024 * 1024) {
                                    byte[] dataInByteArr = new byte[dataLen];
                                    dataIn.readFully(dataInByteArr, 0, dataLen);
                                    outputStream.write(dataInByteArr, 0, dataLen);
                                    Common.Message msg = ByteArrayToComMsg(dataInByteArr);
                                    if (msg != null)
                                        queueBVFromTCP.add(msg);
                                }
                            }
                        }
                        dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                    bvModule.connectionStatus = false;
                    bvModule.userAuthenStatus = false;
                    log.error("DISCONNECTED BECAUSE EXCEPTION");
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        });
        threadReadingData.start();
    }

    public byte[] ComMsgToByteArray(Common.Message inputProtoObj) throws IOException {
        byte[] sslPayload = inputProtoObj.toByteArray();
        byte[] sslLength = intToByteArray(sslPayload.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(sslLength);
        outputStream.write(sslPayload);
        return sslPayload;
    }

    public Common.Message ByteArrayToComMsg(byte[] inputByteArr) {
        try {
            Common.Message msg = Common.Message.parseFrom(inputByteArr);
//            log.info(msg.getType());
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String byteArrayToHexString(byte[] input){
        StringBuilder sb = new StringBuilder();

        for(byte b : input){
            sb.append(String.format("%02x", b & 0xff));
        }
        return new String(sb);
    }

    public byte[] intToByteArray(int input) {
        byte[] a = new byte[4];
        a[0] = (byte)(input >> 24);
        a[1] = (byte)(input >> 16);
        a[2] = (byte)(input >> 8);
        a[3] = (byte)(input);
        return a;
    }

    public int ByteArrayToInt(byte[] input) {
        return (input[3] << 24) | (input[2] << 16) | (input[1] << 8) | input[0];
    }
}
