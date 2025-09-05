package com.vht.connection.TCPConnectionHandler;


import com.google.protobuf.InvalidProtocolBufferException;
import com.vht.connection.BVManager;
import com.vht.connection.Objects.BVModule;
import com.vht.connection.Process.T24Processing;
import com.vht.connection.ReadConfigFile;
import com.vht.connection.manager.CoverageManage;
import com.vht.connection.manager.DeviceMonitor;
import com.vht.connection.manager.InitConnection;
import com.vht.connection.manager.Reconnaissance;
import lombok.extern.log4j.Log4j2;
import vea.api.Common;
import vea.api.command.AssignTarget;
import vea.api.connection.HeartbeatOuterClass;
import vea.api.coverage.CoverageOuterClass;
import vea.api.data.target.DroneOuterClass;
import vea.api.df.Df;
import vea.api.fusion.TargetListOuterClass;
import vea.api.user.UserOuterClass;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ProcessData extends Thread {
    String bv5Id;

    public  ProcessData (String bv5Id) {
        this.bv5Id = bv5Id;
    }

    @Override
    public void run() {
        while (true) {
            try {

                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                BVModule module = BVManager.getInstance().getBVModuleById(bv5Id);
                if(module!= null && !module.handleTCPConnection.queueBVFromTCP.isEmpty()) {
                    exactComMessage(Objects.requireNonNull(module.handleTCPConnection.queueBVFromTCP.poll()));
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void exactComMessage(Common.Message msg) throws InvalidProtocolBufferException {
        if(msg.getPayload().is(HeartbeatOuterClass.Heartbeat.class)) {
            InitConnection.handleConnectionSttFromTCP(bv5Id, msg);
        }
        else if (msg.getPayload().is(UserOuterClass.User.class)) {
            InitConnection.handleUserAuthSttFromTCP(bv5Id, msg);
        }
        else if (msg.getType() == Common.Type.TYPE_SYSTEM) {
            DeviceMonitor.handleMonitorSystemFromTCP(bv5Id, msg);
        }
        else if (msg.getPayload().is(AssignTarget.CommandList.class)) {
            if(msg.getType() == Common.Type.TYPE_COMMAND) {
                log.info("[ProcessData] Command response of " + bv5Id);
                T24Processing.getInstance().ResponseCommandBV5(msg);
            }
        }
        else if (msg.getPayload().is(TargetListOuterClass.TargetList.class)) {
            log.info("[ProcessData] Fusion data of " + bv5Id);
//            log.info("Time receive target from " + bv5Id + ": " + Instant.now().atOffset(ZoneOffset.ofHours(7)).toString());
            Reconnaissance.handleFusionTargetFromTCP(bv5Id, msg);
        }
        else if (msg.getType() == Common.Type.TYPE_DRONE) {
//            log.info("[ProcessData] Drone data of " + bv5Id);
////            log.info("Time receive target from " + bv5Id + ": " + Instant.now().atOffset(ZoneOffset.ofHours(7)).toString());
//            Reconnaissance.handleDroneTargetFromTCP(bv5Id, msg);
        }
        else if(msg.getPayload().is(Df.DfList.class)) {
            if(!ReadConfigFile.turnOffDf) {
                DeviceMonitor.handleDFData(bv5Id, msg);
            }
        }
        else if(msg.getPayload().is(CoverageOuterClass.CoverageList.class)){
            CoverageManage.handleCoverageData(bv5Id, msg);
        }
    }


}