package com.vht.connection.UDPConnectionHandler;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

//import static com.vht.connection.ReadConfigFile.udpPort;

public class UDPClient {
    private static UDPClient udpClient = null;
    private DatagramSocket socket;
    private InetAddress address;
    private String IPHost;
    private byte[] buf;

    public UDPClient(String IPHost) {
        this.IPHost = IPHost;
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(IPHost);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        buf = msg.getBytes(StandardCharsets.ISO_8859_1);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, udpPort);
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
