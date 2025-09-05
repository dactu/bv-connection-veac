package com.vht.connection.Objects;

import com.vht.connection.TCPConnectionHandler.HandleTCPConnection;
import com.vht.connection.TCPConnectionHandler.ProcessData;
import com.vht.connection.TCPConnectionHandler.TCPServer;
import com.vht.connection.UDPConnectionHandler.UDPClient;
import com.vht.connection.manager.CoverageManage;
import com.vht.connection.manager.DeviceMonitor;
import com.vht.connection.manager.InitConnection;
import com.vht.connection.manager.Reconnaissance;
import lombok.Data;
import vea.api.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.vht.connection.Process.T24Processing.MAX_SIZE;

@Data
public class BVModule {
    public String moduleName; // = "BV01"/"BV02"
    public String moduleId;
    public String ipAddress;
    public int port;
    public double latitude;
    public double longitude;

    public int countLossJamming;
    public int countLossEmp;

    public int countEmptyDf;

    public boolean connectionStatus;
    public boolean userAuthenStatus;
    public boolean receiveCoverageStatus;

    public boolean enableConnect = true;

    InitConnection initConnection;
    Reconnaissance reconnaissance;
    ProcessData processData;
    DeviceMonitor deviceMonitor;
    UDPClient udpClient;
    CoverageManage coverageManage;
    public HandleTCPConnection handleTCPConnection;
    public ConcurrentMap<Long, StationContinuousSignalAnalyzer> mapStationContinuous = new ConcurrentHashMap<>();
    public BlockingQueue<Common.Message> commandQueue = new ArrayBlockingQueue<>(MAX_SIZE);

    public BVModule(String moduleName, String moduleId, String ip, int port, double latitude, double longitude) {
        this.moduleName = moduleName;
        this.moduleId = moduleId;
        this.ipAddress = ip;
        this.port = port;
        this.latitude = latitude;
        this.longitude = longitude;
        handleTCPConnection = new HandleTCPConnection(ip, port);
        udpClient = new UDPClient(ip);

        initConnection = new InitConnection(moduleId);
        initConnection.start();
        handleTCPConnection.receiveData(moduleId);

        reconnaissance = new Reconnaissance(moduleId);
        reconnaissance.threadProcessCommand();

        processData = new ProcessData(moduleId);
        processData.start();

//        deviceMonitor = new DeviceMonitor(moduleId);
//        deviceMonitor.start();

//        coverageManage = new CoverageManage(moduleId);
//        coverageManage.start();
    }

    public void putNewStationToBv(long stationId, StationContinuousSignalAnalyzer stationInfo){
        if(mapStationContinuous.containsKey(stationId)){
            mapStationContinuous.replace(stationId, stationInfo);
        } else{
            mapStationContinuous.put(stationId, stationInfo);
        }
    }

    public StationContinuousSignalAnalyzer getStationById(long stationId) {
        return mapStationContinuous.getOrDefault(stationId, null);
    }

    public static class StationContinuousSignalAnalyzer {
        public long id;
        public double latitude;
        public double longitude;
        public String name;
        public ConcurrentMap<Long, DfDevice> mapDfDeviceId = new ConcurrentHashMap<>();

        @Override
        public String toString() {
            return "StationContinuousSignalAnalyzer{" +
                    "id=" + id +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }

        public void putDeviceToMapStation(long devId, DfDevice dfDevice) {
            if(mapDfDeviceId.containsKey(devId)){
                mapDfDeviceId.replace(devId, dfDevice);
            } else{
                mapDfDeviceId.putIfAbsent(devId, dfDevice);
            }
        }
    }

    @Override
    public String toString() {
        return "BVModule{" +
                "moduleName='" + moduleName + '\'' +
                ", moduleId=" + moduleId +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", connectionStatus=" + connectionStatus +
                ", userAuthenStatus=" + userAuthenStatus +
                '}';
    }
}
