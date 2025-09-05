package com.vht.connection.heartbeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class HeartBeatSender  implements Runnable{
    private String[] ipAddress;
    int port;

    public HeartBeatSender(String[] ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }
    @Override
    public void run() {
        while(true) {
            try (DatagramSocket socket = new DatagramSocket()) {
                while (true) {
                    for (String ip : ipAddress) {
                        byte[] buf = "heartbeat".getBytes();
                        InetAddress address = InetAddress.getByName(ip);
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                        socket.send(packet);
                    }
                    Thread.sleep(1000);
                }
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
