package com.vht.connection;

import com.c4i.vq2.messages.RadarCancellationTrackMessage;
import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.BVModule;
import com.vht.connection.Objects.DfArea;
import com.vht.connection.Objects.DfDevice;
import lombok.Data;
import track.T24;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class BVManager {
    static BVManager instance = null;
    public ConcurrentMap<String, BVModule> mapConnection = new ConcurrentHashMap<>();
    public ConcurrentLinkedQueue<RadarCancellationTrackMessage> listCancellationTrackMessage = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> listCancellationBv5PlotMessage = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<T24.QdtPoint> listBv5PlotMessage = new ConcurrentLinkedQueue<>();

    public static BVManager getInstance() {
        synchronized (BVManager.class) {
            if (instance == null) {
                instance = new BVManager();
            }
        }
        return instance;
    }

    public void addBVModule(BVModule module) {
        mapConnection.put(module.getModuleId(), module);
    }

    public BVModule getBVModuleById(String indexBV) {
        return mapConnection.getOrDefault(indexBV, null);
    }

    public void start() {
        Thread threadStatus = new Thread(()-> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (BVModule bv : mapConnection.values()) {
                    if(bv.connectionStatus && bv.userAuthenStatus){
                        GrpcChannel.getInstance().updateStatusBV5(bv.moduleName, 1);
                    }
                    else{
                        GrpcChannel.getInstance().updateStatusBV5(bv.moduleName, 2);

                        if(ReadConfigFile.enableLogEmpJammer){
                            log.warn(bv.moduleName + "|||    Hidden Jamming, EMP and DF due to disconnection!!!");
                        }

                        for(int typeEMPJaming=1; typeEMPJaming<=2; typeEMPJaming++){
                            T24.VungCheAp.Builder vungGNCABuilder = T24.VungCheAp.newBuilder()
                                    .setType(typeEMPJaming)
                                    .setStatus(1)
                                    .setAngleStart(-5)
                                    .setAngleEnd(5)
                                    .setDistance(0)
                                    .setKhiTaiLatitude(0)
                                    .setKhiTaiLongitude(0);

                            if(bv.moduleName.equals(ReadConfigFile.veaName1)) {
                                // che ap
                                vungGNCABuilder.setKiTaiName(ReadConfigFile.veaEmpName1)
                                        .setKhiTaiId(ReadConfigFile.veaEmpName1);

                                // gay nhieu
                                vungGNCABuilder.setDistance(ReadConfigFile.rangeJammer1)
                                        .setKiTaiName(ReadConfigFile.veaJmName1)
                                        .setKhiTaiId(ReadConfigFile.veaJmName1); // by sonlh15
                            } else if(bv.moduleName.equals(ReadConfigFile.veaName2)) {
                                // che ap
                                vungGNCABuilder.setKiTaiName(ReadConfigFile.veaEmpName2)
                                        .setKhiTaiId(ReadConfigFile.veaEmpName2);

                                //gay nhieu
                                vungGNCABuilder.setDistance(ReadConfigFile.rangeJammer2)
                                        .setKiTaiName(ReadConfigFile.veaJmName2)
                                        .setKhiTaiId(ReadConfigFile.veaJmName2); // by sonlh15
                            }

                            GrpcChannel.getInstance().sendEmpJammer(vungGNCABuilder.build());
                        }

                        for(BVModule.StationContinuousSignalAnalyzer station : bv.mapStationContinuous.values()){
                            for(DfDevice device : station.mapDfDeviceId.values()){
                                ConcurrentMap<Long, DfArea> mapDfDelete = device.mapDfArea;
                                for(DfArea df : mapDfDelete.values()){
                                    log.error(bv.moduleName + "|||      Delete do mat ket noi");
                                    GrpcChannel.getInstance().removeExistDf((int) df.m_idArea, station.name, bv.moduleName);
                                }
                            }
                        }
                        bv.mapStationContinuous.clear();
                    }

                }
            }
        });


//        Thread threadBv5Plots = new Thread(()-> {
//            while (true) {
//                try {
//                    TimeUnit.SECONDS.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if(!listBv5PlotMessage.isEmpty()){
//                    GrpcChannel.getInstance().createBv5Plot(listBv5PlotMessage.poll());
//                }
//                if (!listCancellationBv5PlotMessage.isEmpty()){
//                    GrpcChannel.getInstance().deleteBv5Plot(listCancellationBv5PlotMessage.poll());
//                }
//
//
//            }
//        });


        threadStatus.start();
//        threadBv5Plots.start();
    }

    public void threadProcessBv5Plots(){

    }
}
