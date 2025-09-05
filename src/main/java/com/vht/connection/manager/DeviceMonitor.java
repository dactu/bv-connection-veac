package com.vht.connection.manager;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.vht.connection.BVManager;
import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.BVModule;
import com.vht.connection.Objects.DfArea;
import com.vht.connection.Objects.DfDevice;
import com.vht.connection.ReadConfigFile;
import lombok.extern.log4j.Log4j2;
import track.T24;
import vea.api.Common;
import vea.api.df.Df;
import vea.api.sys.StatusOuterClass;
import vea.api.sys.status.QueryOuterClass;
import vea.api.sys.status.StationOuterClass;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Log4j2
public class DeviceMonitor extends Thread{

    String bv5Id;

    public DeviceMonitor(String bv5Id){
        this.bv5Id = bv5Id;
        if(BVManager.getInstance().getBVModuleById(bv5Id)!=null){
            BVManager.getInstance().getBVModuleById(bv5Id).countLossJamming = 0;
            BVManager.getInstance().getBVModuleById(bv5Id).countLossEmp = 0;
            BVManager.getInstance().getBVModuleById(bv5Id).countEmptyDf = 0;
        }
    }

    @Override
    public void run() {
        GrpcChannel.getInstance().removeAllDfAreaWhenStart(bv5Id);
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(BVManager.getInstance().mapConnection.containsKey(bv5Id)){
                if (!BVManager.getInstance().getBVModuleById(bv5Id).userAuthenStatus)
                    continue;
            }
            sendQuerySystem();
        }
    }

    //------------------------------- 3. Giam sat he thong--------------------------------------------------
    public static void handleMonitorSystemFromTCP(String indexBV, Common.Message msg) throws InvalidProtocolBufferException {
        StatusOuterClass.Status systemStatusData = StatusOuterClass.Status.parseFrom(msg.getPayload().getValue());

//        log.info(systemStatusData);
        int numOfStation = systemStatusData.getStationCount();

        int empStatus = 1;
        double empPanAngle = 0;
        StationOuterClass.Station empStation = StationOuterClass.Station.newBuilder()
                .setLocation(Common.Location.newBuilder().build())
                .build();

        int jammerStatus = 1;
        double jammerPanAngle = 0;
        StationOuterClass.Station jammerStation = StationOuterClass.Station.newBuilder()
                .setLocation(Common.Location.newBuilder().build())
                .build();
        //add by quyetdd3
        if(numOfStation<=0){
            if(ReadConfigFile.enableLogEmpJammer){
//                log.info("numbers station = 0");
            }
            return;
        }
        // end add by quyetdd3

        for(int i = 0; i < numOfStation; i++) {
            StationOuterClass.Station station = systemStatusData.getStation(i);
            StationOuterClass.StationType typeStation = station.getType();

            // add StationContinuousSignalAnalyzer
            // change by quyetdd3
            int numListDfInfo = station.getControllerCount();
            for(int j = 0; j < numListDfInfo; j++){
                BVModule.StationContinuousSignalAnalyzer newStationContinuous = new BVModule.StationContinuousSignalAnalyzer();
                newStationContinuous.id = station.getId();
                newStationContinuous.latitude = station.getLocation().getLatitude();
                newStationContinuous.longitude = station.getLocation().getLongitude();
                newStationContinuous.name = station.getDescription();
                BVManager.getInstance().getBVModuleById(indexBV).putNewStationToBv(newStationContinuous.id, newStationContinuous);
            }
            // end change by quyetdd3

//            log.info("LOG: " + BVManager.getInstance().getBVModuleById(indexBV).mapStationContinuous);
            //--------------------VUNG CHE AP----------------------------------------------------
            int numListEmpInfo = station.getEmpCount();
            for(int j=0; j<numListEmpInfo; j++) {
                boolean isMotorOnline = station.getEmp(j).getMotor().getIsConnected();
                boolean isEmpJamming = station.getEmp(j).getIsJamming();

                if(isMotorOnline) {
                    BVManager.getInstance().getBVModuleById(indexBV).countLossEmp = 0;
                    empStation = station;
                    if (!isEmpJamming) {
                        empStatus = 2;
                    } else {
                        empStatus = 3;
                    }
                    empPanAngle = station.getEmp(j).getMotor().getPan();
                }
            }

            //--------------------VUNG GAY NHIEU----------------------------------------------------
            int numListJammerInfo = station.getJammerCount();
            if(numListJammerInfo>0){
                int jammerIdConnected = -1;
                int jammerIdJamming = -1;
                for(int j=0; j<numListJammerInfo; j++) {
                    boolean isMotorOnline = station.getJammer(j).getMotor().getIsConnected();
                    boolean isJammerJamming = station.getJammer(j).getIsJamming();
                    if(isMotorOnline){
                        jammerIdConnected = j;
                    }
                    if(isJammerJamming){
                        jammerIdJamming = j;
                    }
                }
                if(jammerIdConnected != -1){
                    BVManager.getInstance().getBVModuleById(indexBV).countLossJamming = 0;
                    jammerStation = station;
                    jammerPanAngle = station.getJammer(jammerIdConnected).getMotor().getPan();
                    jammerStatus = 2;
                    if(jammerIdJamming != -1) {
                        jammerStatus = 3;
                    }
                }
            }
        }

        T24.VungCheAp.Builder vungCheApBuilder = T24.VungCheAp.newBuilder()
                .setType(1)
                .setStatus(empStatus)
                .setAngleStart(empPanAngle - 3)  // edit follow recommend of Mr Binh(TT & TCDT)
                .setAngleEnd(empPanAngle + 3)
//                .setKhiTaiId(indexBV)
//                .setKiTaiName(indexBV)
                .setDistance(ReadConfigFile.rangeEMP) // by quyetdd3
                .setKhiTaiLatitude(empStation.getLocation().getLatitude())
                .setKhiTaiLongitude(empStation.getLocation().getLongitude());

        T24.VungCheAp.Builder vungGayNhieuBuilder = T24.VungCheAp.newBuilder()
                .setType(2)
                .setStatus(jammerStatus)
                .setAngleStart(jammerPanAngle - 5)
                .setAngleEnd(jammerPanAngle + 5)
//                .setKhiTaiId(indexBV)
//                .setKiTaiName(indexBV)
                .setKhiTaiLatitude(jammerStation.getLocation().getLatitude())
                .setKhiTaiLongitude(jammerStation.getLocation().getLongitude());
        if(indexBV.equals(ReadConfigFile.veaName1)) {
            // che ap
            vungCheApBuilder.setKiTaiName(ReadConfigFile.veaEmpName1)
                    .setKhiTaiId(ReadConfigFile.veaEmpName1);

            // gay nhieu
            vungGayNhieuBuilder.setDistance(ReadConfigFile.rangeJammer1)
                    .setKiTaiName(ReadConfigFile.veaJmName1)
                    .setKhiTaiId(ReadConfigFile.veaJmName1); // by sonlh15
        } else if(indexBV.equals(ReadConfigFile.veaName2)) {
            // che ap
            vungCheApBuilder.setKiTaiName(ReadConfigFile.veaEmpName2)
                    .setKhiTaiId(ReadConfigFile.veaEmpName2);

            //gay nhieu
            vungGayNhieuBuilder.setDistance(ReadConfigFile.rangeJammer2)
                    .setKiTaiName(ReadConfigFile.veaJmName2)
                    .setKhiTaiId(ReadConfigFile.veaJmName2); // by sonlh15
        }
        log.info(indexBV + "|||    Sending VUNG CHE AP info ...");
        log.info(vungCheApBuilder.build().toString());
        log.info(indexBV + "|||    Sending VUNG GAY NHIEU info ...");
        log.info(vungGayNhieuBuilder.build().toString());

        if(empStatus==1){
            GrpcChannel.getInstance().sendEmpJammer(vungCheApBuilder.build());
            log.info(indexBV + "|||    SENT VUNG CHE AP SUCCESSFULLY, STATUS = 1");
        }
        else{
            BVManager.getInstance().getBVModuleById(indexBV).countLossEmp = 0;
            GrpcChannel.getInstance().sendEmpJammer(vungCheApBuilder.build());
            log.info(indexBV + "|||    SENT VUNG CHE AP SUCCESSFULLY, STATUS = " + empStatus);
        }

        if(jammerStatus == 1){
            GrpcChannel.getInstance().sendEmpJammer(vungGayNhieuBuilder.build());
            log.info(indexBV + "|||    SENT VUNG GAY NHIEU SUCCESSFULLY, STATUS = 1");
        }
        else{
            BVManager.getInstance().getBVModuleById(indexBV).countLossJamming = 0;
            GrpcChannel.getInstance().sendEmpJammer(vungGayNhieuBuilder.build());
            log.info(indexBV + "|||    SENT VUNG GAY NHIEU SUCCESSFULLY, STATUS = " + jammerStatus);
        }
    }

    public static void handleDFData(String indexBV, Common.Message msg) {
        try {
            Df.DfList dfList = Df.DfList.parseFrom(msg.getPayload().getValue());
            BVModule bvModule = BVManager.getInstance().getBVModuleById(indexBV);
            long stationSourceId = dfList.getSource().getStation();
            long deviceId = dfList.getSource().getDevice();
            if(bvModule.mapStationContinuous.containsKey(stationSourceId)) {
                //Get station info
                double stationLat = bvModule.mapStationContinuous.get(stationSourceId).latitude;
                double stationLong = bvModule.mapStationContinuous.get(stationSourceId).longitude;

                String stationName = bvModule.mapStationContinuous.get(stationSourceId).name;
                if(!bvModule.mapStationContinuous.get(stationSourceId).name.contains(String.valueOf(deviceId))
                    && !bvModule.mapStationContinuous.get(stationSourceId).name.contains("[")) {
                    stationName = "[" + deviceId + "] " + stationName;
                }

                //Check if device is new
                boolean isNewDevice = false;
                if(!bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.containsKey(deviceId)){
                    //Neu khong -> put vao map
                    DfDevice device = new DfDevice();
                    bvModule.mapStationContinuous.get(stationSourceId).putDeviceToMapStation(deviceId, device);
                    isNewDevice = true;
                }

                //Kiem tra list tia DF nhan duoc
                //Neu count > 0 -> duyet List
                //Else (list rong) -> xoa het tia DF co station ID, device ID do, day khoi map
                if(dfList.getDfListCount() > 0) {
                    //Duyet list DF nhan duoc
                    for (Df.DF df : dfList.getDfListList()) {
                        T24.VungTrinhSat.Builder vungTrinhSatBuilder = T24.VungTrinhSat.newBuilder()
                                .setIDReQuat((int) df.getId())
                                .setTramId(String.valueOf(stationSourceId))
                                .setTramName(stationName)
                                .setTramLatitude(stationLat)
                                .setTramLongitude(stationLong)
                                .setKhiTaiId(indexBV)
                                .setKhiTaiName(indexBV)
                                .setDistance(ReadConfigFile.rangeDf)
                                .setGocPhuongVi(df.getAzimuth())
                                .setGocTa(df.getElevation())
                                .setBangThong( (double) (df.getFcEnd() - df.getFcBegin()) / (Math.pow(10, 6)))
                                .setTanSoTrungTam( (double) (df.getFcEnd() + df.getFcBegin()) / (2* (Math.pow(10, 6))) )
                                .setAngleStart( df.getAzimuth() - ReadConfigFile.openAngleDf)
                                .setAngleEnd( df.getAzimuth() + ReadConfigFile.openAngleDf);

                        String idCheckDfExist =  GrpcChannel.getInstance().searchVungTrinhSatByIdAtStart((int)df.getId(), stationName, indexBV);

                        if( (!isNewDevice && bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea.containsKey(df.getId())) || idCheckDfExist!=null){
                            if(df.getState() == Df.DfTrackState.DF_TRACK_INGRAVE || df.getState() == Df.DfTrackState.DF_TRACK_DELETE){
                                log.error(bvModule.getModuleId() + "|||      Delete do gap track state delete");
                                GrpcChannel.getInstance().removeExistDf((int) df.getId(), stationName, indexBV);
                                bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea.remove(df.getId());
                            } else{
                                vungTrinhSatBuilder.setStatus(3);
                                bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea.get(df.getId()).isLoss = false;
                                GrpcChannel.getInstance().patchExistDfArea((int) df.getId(), indexBV, vungTrinhSatBuilder.build());
                            }
                        } else{
                            if(indexBV.contains("BV5.02")){
                                log.info("here");
                            }
                            DfArea newDfInfo = new DfArea();
                            newDfInfo.m_idArea = df.getId();
                            newDfInfo.m_azimuth = df.getAzimuth();
                            newDfInfo.m_elevation = df.getElevation();
                            newDfInfo.m_fc_begin = df.getFcBegin();
                            newDfInfo.m_fc_end = df.getFcEnd();

                            DfDevice dfDev = BVManager.getInstance().getBVModuleById(indexBV).mapStationContinuous
                                    .get(stationSourceId).mapDfDeviceId.get(deviceId);
                            dfDev.putDfInfoToMapDevice(newDfInfo.m_idArea, newDfInfo);
                            BVManager.getInstance().getBVModuleById(indexBV).mapStationContinuous
                                    .get(stationSourceId).putDeviceToMapStation(deviceId, dfDev);
                            GrpcChannel.getInstance().createNewDfArea(vungTrinhSatBuilder.setStatus(3).build());
                        }
                    }

                    for(int idx = 0; idx < bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.size(); idx++){
                        ConcurrentMap<Long, DfArea> mapDfDelete = bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea;
                        for(DfArea df : mapDfDelete.values()){
                            if(df.isLoss){
                                log.error(bvModule.getModuleId() + "|||      Delete do k gap ban tin khac");
                                GrpcChannel.getInstance().removeExistDf((int) df.m_idArea, stationName, indexBV);
                                bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea.remove(df.m_idArea);
                            }
                        }
                    }
                }
                else{
                    for(int idx = 0; idx < bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.size(); idx++){
                        ConcurrentMap<Long, DfArea> mapDfDelete = bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea;
                        for(DfArea df : mapDfDelete.values()){
                            log.error(bvModule.getModuleId() + "|||      Delete do list rong");
                            GrpcChannel.getInstance().removeExistDf((int) df.m_idArea, stationName, indexBV);
                            bvModule.mapStationContinuous.get(stationSourceId).mapDfDeviceId.get(deviceId).mapDfArea.remove(df.m_idArea);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendQuerySystem() {
//        log.info("Querying system status ...");
        BVManager.getInstance().getBVModuleById(bv5Id).handleTCPConnection.sendData(bv5Id, getQueryRequest());
    }

    public Common.Message getQueryRequest() {
        Common.Transaction transactionData = Common.Transaction.newBuilder()
                .setId(InitConnection.currentSendMessageId++)
                .setTimeout(2000)
                .build();

        QueryOuterClass.Query queryData = QueryOuterClass.Query.newBuilder()
                .setTransaction(transactionData)
                .build();

        return Common.Message.newBuilder()
                .setType(Common.Type.TYPE_SYSTEM)
                .setSubtype(1)
                .setPayload(Any.pack(queryData))
                .setTransaction(transactionData)
                .build();
    }
}
