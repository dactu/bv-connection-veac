package com.vht.connection;

import com.vht.connection.Asterix.AsterixCat48Encoder;
import com.vht.connection.Interface.ConnectionNatIO;
import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.BVModule;
//import com.vht.connection.Process.ModuleProcessing;
import com.vht.connection.Process.T24Processing;
import com.vht.connection.TCPConnectionHandler.ModuleStatusManager;
import com.vht.connection.TCPConnectionHandler.TCPServer;
import com.vht.connection.Vq.Manager;
import com.vht.connection.Vq.TcpClient;
import com.vht.connection.heartbeat.PriorityManager;
import com.vht.connection.testMultiClient.FirstClient;
import com.vht.connection.testMultiClient.SecondClient;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

import static com.vht.connection.ReadConfigFile.*;

@Log4j2
@SpringBootApplication
@EnableScheduling
public class TrainingConnectionApplication {
    private static final Logger log = LoggerFactory.getLogger(TrainingConnectionApplication.class);

    public static void main(String[] args) throws IOException {
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        SpringApplication.run(TrainingConnectionApplication.class, args);

//        T24Processing.getInstance().initT24Processing();
//        GrpcChannel.getInstance().onCreateChannel();
//        GrpcChannel.getInstance().clearAllModuleStatusAtStart();
        log.info("Name class: {}", String.valueOf(TrainingConnectionApplication.class));

//        BVModule module = new BVModule(veaName1, veaName1, veaIpAddress1, veaPort1, veaLat1, veaLong1);
//        log.info("CREATE BV5 " + module);
//        BVManager.getInstance().addBVModule(module);
//
//        BVModule module2 = new BVModule(veaName2, veaName2, veaIpAddress2, veaPort2, veaLat2, veaLong2);
//        log.info("CREATE BV5 " + module2);
//        BVManager.getInstance().addBVModule(module2);

        BVManager.getInstance().start();

//        TCPServer.getInstance().createServer(moduleStatusPort);
//        TCPServer.getInstance().start();
//        ModuleProcessing.getInstance().initModuleProcessing();
//        if(enableLogModuleStatus) {
//            TCPServer tcpServer = new TCPServer(moduleStatusPort);
//            tcpServer.start();
//            ModuleStatusManager moduleStatusManager = new ModuleStatusManager(tcpServer);
//            moduleStatusManager.initModuleProcessing();
//        }
//        if(!turnOffVq) {
//            TcpClient tcpClient = new TcpClient(empGrpcAddress, 20100);
//            tcpClient.start();
//            Manager manager = new Manager(tcpClient);
//            manager.start();
//        }
//        PriorityManager priorityManager = new PriorityManager();
//        priorityManager.checkListIP();
//        priorityManager.process();

    }
}
