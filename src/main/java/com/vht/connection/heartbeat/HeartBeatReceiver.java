package com.vht.connection.heartbeat;

import com.vht.connection.ReadConfigFile;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
public class HeartBeatReceiver implements Runnable {
    Map<String, Boolean> heartbeatMap = new HashMap<>();
    int port;
    volatile boolean heartbeatReceived;
    static boolean enableSendTargetToBv5;
    static boolean isMainServerAlive;
    static boolean isDp01ServerAlive;
    static boolean isDp02ServerAlive;
    static String serverPriority;
    public HeartBeatReceiver(int port, String[] ipAddress) {
        this.port = port;
        for (String ip : ipAddress){
            heartbeatMap.put(ip, false);
        }
    }

    public boolean isHeartbeatReceived(String ip) {
        return heartbeatMap.getOrDefault(ip, false);
    }

     public void resetHeartbeat(){
        for(String ip : heartbeatMap.keySet()){
            heartbeatMap.put(ip, false);
        }
     }

     @Override
    public void run() {
         while (true) {
             try (DatagramSocket socket = new DatagramSocket(port)) {
                 byte[] buf = new byte[256];
                 while (true) {
                     DatagramPacket packet = new DatagramPacket(buf, buf.length);
                     try {
                         socket.setSoTimeout(5000);
                         socket.receive(packet);
                         String received = new String(packet.getData(), 0, packet.getLength());
                         if ("heartbeat".equals(received) && heartbeatMap.containsKey(packet.getAddress().getHostAddress())) {
                             heartbeatMap.put(packet.getAddress().getHostAddress(), true);
                         }
                     } catch (SocketException e){
                         log.error("Socket closed");
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     Thread.sleep(10);
                 }
             } catch (SocketException e) {
                 log.error("Socket closed, exiting.");
             } catch (IOException | InterruptedException e) {
                 e.printStackTrace();
             }
             try {
                 TimeUnit.MILLISECONDS.sleep(10);
             } catch (InterruptedException e) {
                 throw new RuntimeException(e);
             }
         }
     }
}
