package com.vht.connection.manager;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.vht.connection.BVManager;
import com.vht.connection.Objects.BVModule;
import com.vht.connection.ReadConfigFile;
import lombok.extern.log4j.Log4j2;
import vea.api.Common;
import vea.api.command.AssignTarget;
import vea.api.connection.HeartbeatOuterClass;
import vea.api.user.AuthenticateOuterClass;
import vea.api.user.UserOuterClass;

import java.util.concurrent.TimeUnit;

@Log4j2
public class InitConnection extends Thread{
    //Loop Period
    String bv5Id;

    //Monitor heartbeat
    public static final int TIMEOUT_HEARTBEAT = 2;   // 50 * LOOP DELAY = 5000 = 5s (Chu ky gui heartbeat)
    private int cntBeat = 0;

    public static int currentSendMessageId = 0;

    public static int currentCmdId = 0;

    public InitConnection(String bv5Id) {
        this.bv5Id = bv5Id;
    }

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //BMS-Client connect TCP to VEA/C-Server
            if(BVManager.getInstance().mapConnection.containsKey(bv5Id)) {
                log.info(BVManager.getInstance().getBVModuleById(bv5Id).toString());
                BVModule connModule = BVManager.getInstance().getBVModuleById(bv5Id);
                if(connModule.enableConnect){
                    if(connModule.connectionStatus) {
                        if(!connModule.userAuthenStatus) {
                            log.warn("AUTHENTICATION");
                            handleAuthenticate();
                        } else {
                            handleHeartBeat();     //Loop heart_beat
                        }
                    } else {
                        connModule.handleTCPConnection.createSSLSocket(bv5Id);
                    }
                }
                else{
                    connModule.connectionStatus = false;
                    connModule.userAuthenStatus = false;
                }
            }
        }
    }

    //------------------------- 1. HANDSHAKE -------------------------------------------------------------
    public void handleAuthenticate() {
        log.info("Authenticating user with " + bv5Id);
        BVModule connModule = BVManager.getInstance().getBVModuleById(bv5Id);

        connModule.handleTCPConnection.sendData(bv5Id, getAuthenRequest());
    }

    public Common.Message getAuthenRequest(){
        Common.Error errorData = Common.Error.newBuilder().setCode(1).setMessage("No error").build();

        Common.Transaction transactionData = Common.Transaction.newBuilder()
                .setId(currentSendMessageId++)
                .setError(errorData)
                .setTimeout(2000)
                .build();

        String user = ReadConfigFile.veriUsername;
        String pass = ReadConfigFile.veriPassword;

        AuthenticateOuterClass.Authenticate authenData = AuthenticateOuterClass.Authenticate.newBuilder()
                .setTransaction(transactionData)
                .setUsername(user)
                .setPassword(pass)
                .build();
        AssignTarget.CommandList.Builder assignTargetBuilder = AssignTarget.CommandList.newBuilder();
        assignTargetBuilder.addCommand(
                AssignTarget.Command.newBuilder()
                        .setCmdId(currentCmdId++)
                        .setCmdType(AssignTarget.AssignTargetType.AT_Jamming)
                        .build());
        return Common.Message.newBuilder()
                .setType(Common.Type.TYPE_USER)
                .setSubtype(1)
                .setPayload(Any.pack(authenData))
                .setTransaction(transactionData)
                .build();
    }

    public static void handleUserAuthSttFromTCP(String indexBV, Common.Message msg) throws InvalidProtocolBufferException {
        BVModule bvModule = BVManager.getInstance().getBVModuleById(indexBV);
        if(bvModule==null){
            log.warn("[handleUserAuthSttFromTCP] BVModule is null");
            return;
        }
        if(bvModule.userAuthenStatus) {
            log.info(indexBV + "||| has already authenticated!!");
        } else {
            if (msg.getSubtype() != 2) {
                UserOuterClass.User userData = UserOuterClass.User.parseFrom(msg.getPayload().getValue());
                log.info(userData.toString());
                bvModule.userAuthenStatus = userData.getIsAuthenticated();
                String debugStr = bvModule.userAuthenStatus ? bvModule.moduleName + ": User is authenticated successfully!!!!!" : bvModule.moduleName + ": Fail to authenticate user, try again";
                log.info(debugStr);
            }
        }
    }

    //------------------------- 2. HEART BEAT -------------------------------------------------------------
    public void handleHeartBeat(){
        if (cntBeat % TIMEOUT_HEARTBEAT == 0){
            cntBeat = 0;
            sendHeartBeat();
        }
        cntBeat++;
    }

    public void sendHeartBeat() {
        //log.info("HeartBeat");
        BVManager.getInstance().getBVModuleById(bv5Id).handleTCPConnection.sendData(bv5Id, getHeartBeatRequest());
    }

    public Common.Message getHeartBeatRequest(){
        Common.Error errorData = Common.Error.newBuilder().setCode(1).setMessage("No error").build();

        Common.Transaction transactionData = Common.Transaction.newBuilder()
                .setId(currentSendMessageId++)
                .setError(errorData)
                .setTimeout(2000)
                .build();

        HeartbeatOuterClass.Heartbeat heartBeat = HeartbeatOuterClass.Heartbeat.newBuilder()
                .setTransaction(transactionData)
                .setVersion("2.0.0")
                .build();

        //log.info(heartBeat.toString());

        return Common.Message.newBuilder()
                .setType(Common.Type.TYPE_CONNECTION)
                .setSubtype(1)
                .setPayload(Any.pack(heartBeat))
                .setTransaction(transactionData)
                .build();
    }

    public static void handleConnectionSttFromTCP(String indexBV, Common.Message msg) throws InvalidProtocolBufferException {
        //log.info("Received heartbeat response from VEA/C!!!");
    }
}

