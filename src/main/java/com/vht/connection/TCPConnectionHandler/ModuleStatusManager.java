package com.vht.connection.TCPConnectionHandler;

import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.ReadConfigFile;
import connection.status.ConnectionStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.log4j.Log4j2;
import track.T24;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ModuleStatusManager {
    TCPServer tcpServer;

    public ModuleStatusManager(TCPServer tcpServer) {
        this.tcpServer = tcpServer;
    }
    public static ConcurrentMap<String, Long> mapModuleStatus = new ConcurrentHashMap<>();

    public static void putModuleStatusInfoToMapDevice(String moduleName, long latestUpdateTime) {
        if(mapModuleStatus.containsKey(moduleName)){
            mapModuleStatus.replace(moduleName, latestUpdateTime);
        } else{
            mapModuleStatus.put(moduleName, latestUpdateTime);
        }
    }

    public void initModuleProcessing(){
        processModuleSttFromTCP();
    }

    public void processModuleSttFromTCP(){
        Thread processModuleSttFromTCPThread = new Thread(() -> {
            log.info("Thread process module status Message");
            int countCheckSize = 0;
            while (true) {
                try {
                    countCheckSize++;
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Neu khong nhan dc du lieu moi trong 10s -> empty tat ca cac truong thong tin Trang thai khi tai.
                for(Map.Entry entry : mapModuleStatus.entrySet()){
                    String key = (String) entry.getKey();
                    if(System.currentTimeMillis() - mapModuleStatus.get(key) > ReadConfigFile.timeoutSendEmptyStatus*1000){
                        GrpcChannel.getInstance().sendEmptyInfo(key);
                    }
                }

                ByteBuf buffer= tcpServer.pollQueue();
                if(buffer!=null) {
                    byte[] data0= ByteBufUtil.getBytes(buffer);
                    try{
                        short startBA = 0x0000;
                        startBA = (short) ((data0[0] << 8) | (data0[1]));

                        if (startBA != 0x0F0F) {
                            log.warn("invalid header: " + startBA);
                            continue;
                        }

                        int dataLen = (data0[2] << 24) | (data0[3] << 16) | (data0[4] << 8) | (data0[5]);
                        if (dataLen > 0) {
                            byte[] dataInByteArr = new byte[dataLen];
                            for(int k=0; k<dataLen; k++){
                                dataInByteArr[k] = data0[k+6];
                            }
                            ConnectionStatus.CSSensorConnection msg = ByteArrayToModuleStMsg(dataInByteArr);
                            if (msg != null) {
                                if (msg.getRadarInforCount() > 0) {
                                    log.info("Receive radar info: ");
                                    for (int i = 0; i < msg.getRadarInforCount(); i++) {
                                        handleRadarInfo(msg.getRadarInfor(i));
                                    }
                                }
                                if (msg.getQdtInforCount() > 0) {
                                    log.info("Receive qdt info: ");
                                    for (int i = 0; i < msg.getQdtInforCount(); i++) {
                                        handleQDTInfo(msg.getQdtInfor(i));
                                    }
                                }
                                if (msg.getBv5InforCount() > 0) {
                                    log.info("Receive bv5 info: ");
                                    for (int i = 0; i < msg.getBv5InforCount(); i++) {
                                        handleBV5Info(msg.getBv5Infor(i));
                                    }
                                }
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                } else {
                    if(countCheckSize>5) {
                        log.info("queue module status empty");
                        countCheckSize =0;
                    }
                }
            }

        });
        processModuleSttFromTCPThread.start();
    }
    public void handleRadarInfo(ConnectionStatus.RadarInfor msg){
        if(ReadConfigFile.enableLogModuleStatus){
            log.info("module radar status: " + msg);
        }
        String moduleName = msg.getName();
        if(msg.getName().toLowerCase().contains("u2x")){
            moduleName = ReadConfigFile.radarU2xName;
        } else if(msg.getName().toLowerCase().contains("u3k")){
            moduleName = ReadConfigFile.radarU3kName;
        } else if(msg.getName().toLowerCase().contains("srs")){
            if(msg.getName().toLowerCase().contains("1")){
                moduleName = ReadConfigFile.radarSrs01Name;
            } else if(msg.getName().toLowerCase().contains("2")){
                moduleName = ReadConfigFile.radarSrs02Name;
            } else if(msg.getName().toLowerCase().contains("3")){
                moduleName = ReadConfigFile.radarSrs03Name;
            }
        } else if(msg.getName().toLowerCase().contains("mrs")) {
            moduleName = ReadConfigFile.radarMrsName;
        }
        T24.TrangThaiKhiTai.Builder moduleSttToSend = T24.TrangThaiKhiTai.newBuilder()
                .setName(moduleName);

        if(!msg.getOperator().equals(ConnectionStatus.CSOperator.OP_UNKNOWN)){
            moduleSttToSend.setSSCD(msg.getOperatorValue())
                    .setCheDoHoatDong(msg.getModeValue())
                    .setStationAltitude(msg.getPosition().getAltitude())
                    .setStationLatitude(msg.getPosition().getLatitude())
                    .setStationLongitude(msg.getPosition().getLongitude())
                    .setTocDoQuet(msg.getScanSpeed())
                    .setCongSuatPhat(msg.getPower())
                    .setCuLyCanhGioi(msg.getRangeCG());
        }

        moduleSttToSend.setType(GrpcChannel.getInstance().searchModuleTypeByName(moduleName));

        GrpcChannel.getInstance().sendTrangThaiKhiTai(moduleSttToSend.build());
        log.info(moduleSttToSend.toString());
        putModuleStatusInfoToMapDevice(moduleName, System.currentTimeMillis());
    }

    public void handleQDTInfo(ConnectionStatus.QDTInfor msg){
        if(ReadConfigFile.enableLogModuleStatus){
            log.info("module qdt status: " + msg);
        }
        String moduleName = msg.getName();
        if(msg.getName().toLowerCase().contains("bv5")){
            if(msg.getName().toLowerCase().contains("1")){
                moduleName = ReadConfigFile.bv5Lra01Name;
            } else if(msg.getName().toLowerCase().contains("2")){
                moduleName = ReadConfigFile.bv5Lra02Name;
            }
        } else {
            if(msg.getName().toLowerCase().contains("1")){
                moduleName = ReadConfigFile.srsLra01Name;
            } else if(msg.getName().toLowerCase().contains("2")){
                moduleName = ReadConfigFile.srsLra02Name;
            } else if(msg.getName().toLowerCase().contains("3")){
                moduleName = ReadConfigFile.srsLra03Name;
            }
        }

        T24.TrangThaiKhiTai.Builder moduleSttToSend = T24.TrangThaiKhiTai.newBuilder()
                .setName(moduleName);

        if(!msg.getMode().equals(ConnectionStatus.CSModeQDT.QDT_UNKNOWN)){
            // Check neu Che do cam ngay -> Ko set truong che do anh nhiet
            if(msg.getMode().equals(ConnectionStatus.CSModeQDT.QDT_Ngay)){
                moduleSttToSend.setCheDoHoatDong(4);
            } else {
                moduleSttToSend.setCheDoHoatDong(5);
                moduleSttToSend.setCheDoAnhNhiet(msg.getModeIRValue());
            }
            int distanceMeasure = (int) msg.getDistanceMeasure();
            if(distanceMeasure<0){
                distanceMeasure=0;
            }
            moduleSttToSend.setSSCD(msg.getOperatorValue())
                    .setStationAltitude(msg.getPosition().getAltitude())
                    .setStationLatitude(msg.getPosition().getLatitude())
                    .setStationLongitude(msg.getPosition().getLongitude())
                    .setMucZoomHienTai(msg.getZoomLevel())
//                .setKetQuaDoXa((int) msg.getDistanceMeasure())
                    .setKetQuaDoXa(distanceMeasure)
                    .setTrangThaiBatBamMucTieu(msg.getTrackingStateValue());

        }
        moduleSttToSend.setType(GrpcChannel.getInstance().searchModuleTypeByName(moduleName));

        GrpcChannel.getInstance().sendTrangThaiKhiTai(moduleSttToSend.build());
        log.info(moduleSttToSend.toString());
        putModuleStatusInfoToMapDevice(moduleName, System.currentTimeMillis());
    }

    public void handleBV5Info(ConnectionStatus.BV5Infor msg){
        if(ReadConfigFile.enableLogModuleStatus){
            log.info("module bv5 status: " + msg);
        }
        String moduleName = "";
        if(msg.getName().contains("1")){
            moduleName = ReadConfigFile.veaName1;
        } else if(msg.getName().contains("2")){
            moduleName = ReadConfigFile.veaName2;
        }

        T24.TrangThaiKhiTai.Builder moduleSttToSend = T24.TrangThaiKhiTai.newBuilder()
                .setName(moduleName)
                .setSSCD(msg.getOperatorValue())
                .setCheDoHoatDong(msg.getModeValue())
                .setStationAltitude(msg.getPosition().getAltitude())
                .setStationLatitude(msg.getPosition().getLatitude())
                .setStationLongitude(msg.getPosition().getLongitude());

        moduleSttToSend.setType(GrpcChannel.getInstance().searchModuleTypeByName(moduleName));
        GrpcChannel.getInstance().sendTrangThaiKhiTai(moduleSttToSend.build());

        putModuleStatusInfoToMapDevice(moduleName, System.currentTimeMillis());
        log.info(moduleSttToSend.toString());
    }

//    public void sendEmptyInfo(String moduleName){
//        T24.TrangThaiKhiTai.Builder moduleSttToSend = T24.TrangThaiKhiTai.newBuilder()
//                .setName(moduleName);
//        moduleSttToSend.setType(GrpcChannel.getInstance().searchModuleTypeByName(moduleName));
//        GrpcChannel.getInstance().sendTrangThaiKhiTai(moduleSttToSend.build());
//        putModuleStatusInfoToMapDevice(moduleName, System.currentTimeMillis());
//    }
    public ConnectionStatus.CSSensorConnection ByteArrayToModuleStMsg(byte[] inputByteArr) {
        try {
            ConnectionStatus.CSSensorConnection msg = ConnectionStatus.CSSensorConnection.parseFrom(inputByteArr);
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
