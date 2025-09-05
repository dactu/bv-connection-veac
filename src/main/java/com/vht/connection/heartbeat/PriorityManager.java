package com.vht.connection.heartbeat;

import com.vht.connection.ReadConfigFile;
import io.nats.client.Message;
import lombok.extern.log4j.Log4j2;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Log4j2
public class PriorityManager {
    static final int port = ReadConfigFile.portHB;
    static final String[] IP_ADDRESS = {ReadConfigFile.mainAddress, ReadConfigFile.dp01Address, ReadConfigFile.dp02Address};
    ConcurrentLinkedQueue<String> listIP = new ConcurrentLinkedQueue<>();

    public static boolean enableSend = false;

    public void checkListIP(){
        Thread listIpThread = new Thread(() -> {
            while (true) {
                try {
                    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                    for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
//                        log.info("interface: " + networkInterface.getName());
                        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                            listIP.add(inetAddress.getHostAddress());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        listIpThread.start();
    }

    public void process(){

        HeartBeatReceiver receiver = new HeartBeatReceiver(port, IP_ADDRESS);
        Thread receiverThread = new Thread(receiver);
        receiverThread.start();

        HeartBeatSender sender = new HeartBeatSender(IP_ADDRESS, port);
        Thread senderThread = new Thread(sender);
        senderThread.start();

        Thread processMessage = new Thread(() -> {
            while (true) {
                try{
                    Thread.sleep(ReadConfigFile.timeoutCheckHB);
                    enableSend = false;
                    for(int i =0; i<IP_ADDRESS.length; i++){
                        String currentIP = IP_ADDRESS[i];
                        if(listIP.contains(currentIP)){
                            if(i==0){
                                log.info("send target to bv5 in server " + currentIP + " is running");
                                enableSend = true;
                            } else if(i==1) {
                                String previousIP = IP_ADDRESS[i-1];
                                if(!receiver.isHeartbeatReceived(previousIP)){
                                    log.info("send target to bv5 in server " + currentIP + " is running");
                                    enableSend = true;
                                }
                            } else {
                                String previousIP = IP_ADDRESS[i-1];
                                String mainIP = IP_ADDRESS[i-2];
                                if((!receiver.isHeartbeatReceived(previousIP))&&(!receiver.isHeartbeatReceived(mainIP))){
                                    log.info("send target to bv5 in server " + currentIP + " is running");
                                    enableSend = true;
                                }
                            }
                            break;
                        }
                    }
                    receiver.resetHeartbeat();
                } catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        processMessage.start();
    }

}
