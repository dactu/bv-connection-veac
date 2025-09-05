package com.vht.connection.Interface;

import com.vht.bms.messages.BaseMessageProto;
import com.vht.connection.Process.T24Processing;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ConnectionNatIO {
    private String uri;
    private static ConnectionNatIO instance;

    private Connection connection;
    private boolean NatsIOCntStt = false;

    public ConnectionNatIO() {
    }

    public static ConnectionNatIO getInstance(){
        if(instance == null){
            instance = new ConnectionNatIO();
        }
        return instance;
    }

    public void initConnection(String hostname) {
        uri = "nats://" + hostname + ":4222";
        Thread initNatsConnectThread = new Thread(() -> {
            while (true){
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(!NatsIOCntStt) {
                    Options options = new Options.Builder()
                            .errorCb(ex -> log.error("Connection Exception: ", ex))
                            .disconnectedCb(event -> {
                                log.error("Channel disconnected: {}", event.getConnection());
                                NatsIOCntStt = false;
                            })
                            .reconnectedCb(event -> {
                                log.info("Reconnected to server: {}", event.getConnection());
                            })
                            .build();

                    try {
                        connection = Nats.connect(uri, options);
                        NatsIOCntStt = true;
                        log.info("Connect successfully to nats io : " + uri);
                        connection.subscribe("bv-tactical.commands.*.*.*", new MessageHandler() {
                            @Override
                            public void onMessage(Message message) {
//                                log.info("received nat message from subject: bv-tactical.commands ");
                                T24Processing.getInstance().receivedMessageFromNatsIO(message);
                            }
                        });
                        connection.subscribe("bv-ipcserver.track", new MessageHandler() {
                            @Override
                            public void onMessage(Message message) {
//                                log.info("received nat message from subject: bv-ipcserver.track");
                                T24Processing.getInstance().receivedMessageFromNatsIO(message);
                            }
                        });
                        connection.subscribe("bv-tactical.khi-tais.*.false.*", new MessageHandler() {
                            @Override
                            public void onMessage(Message message) {
//                                log.info("received nat message from subject: bv-ipcserver.track");
                                T24Processing.getInstance().receivedMessageFromNatsIO(message);
                            }
                        });
                    } catch (IOException e) {
                        NatsIOCntStt = false;
                        connection.close();
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
        initNatsConnectThread.start();
    }

    public void publishMessage(String topic, BaseMessageProto.BaseMessage baseMessage) {
        try {
            if(connection != null && connection.isConnected())
                connection.publish(topic, baseMessage.toByteArray());
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }
}
