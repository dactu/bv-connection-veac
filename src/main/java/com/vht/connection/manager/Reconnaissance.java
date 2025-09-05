package com.vht.connection.manager;

import com.c4i.vq2.messages.RadarCancellationTrackMessage;
import com.c4i.vq2.messages.RadarCancellationTrackMessageOrBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.vht.bms.messages.BaseMessageProto;
import com.vht.bms.messages.PositionAndVelocityProto;
import com.vht.bms.messages.track.TrackInfoProto;
import com.vht.bms.messages.track.TrackMessageProto;
import com.vht.connection.BVManager;
import com.vht.connection.Interface.ConnectionNatIO;
import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.BVModule;
import io.grpc.Grpc;
import lombok.extern.log4j.Log4j2;
import track.T24;
import vea.api.Common;
import vea.api.data.target.DroneOuterClass;
import vea.api.data.target.FusedOuterClass;
import vea.api.fusion.TargetListOuterClass;
import vea.api.fusion.TargetOuterClass;
import com.vht.connection.ReadConfigFile;
import vea.api.sys.status.RadarOuterClass;

import java.util.concurrent.TimeUnit;

import static com.vht.connection.BVManager.listCancellationBv5PlotMessage;
import static vea.api.data.target.FusedOuterClass.FUSED_CLASSIFICATION_TYPE.*;
import static vea.api.data.target.FusedOuterClass.FUSED_TRAJECTORY_STATE.*;

@Log4j2
public class Reconnaissance {

    //Loop Period
    private static final int LOOP_DELAY = 100;
    String bv5Id;

    public Reconnaissance(String bv5Id){
        this.bv5Id = bv5Id;
    }

    public void threadProcessCommand() {
        Thread threadProcessCommandBV = new Thread(()-> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                BVModule bvModule = BVManager.getInstance().getBVModuleById(bv5Id);
                if(bvModule == null){
                    log.warn("BV module not found");
                    continue;
                }
                if(!bvModule.userAuthenStatus) continue;
                if(bvModule.connectionStatus) {
                    if (!bvModule.commandQueue.isEmpty()) {
                        Common.Message commandMsg = bvModule.commandQueue.poll();
                        if(commandMsg != null) {
                            bvModule.handleTCPConnection.sendData(bv5Id, commandMsg);
                        }
                    }
                }
            }
        });
        threadProcessCommandBV.start();
    }
    //************************************* NHOM BAN TIN GIAM SAT **************************************************

    //------------------------------- Fusion(Thong tin muc tieu tu nguon hop nhat)---------------------------------------
    public static void handleFusionTargetFromTCP(String indexBV, Common.Message msg) throws InvalidProtocolBufferException {
        TargetListOuterClass.TargetList fusionTargetInfoList = TargetListOuterClass.TargetList.parseFrom(msg.getPayload().getValue());
        log.info("\n-------------Receive new Fusion track /plot!!--------------- {} fusion track /plot", fusionTargetInfoList.getTargetCount());
        for (int i = 0; i < fusionTargetInfoList.getTargetCount(); i++) {
            BaseMessageProto.BaseMessage.Builder baseMessage = BaseMessageProto.BaseMessage.newBuilder();
            TargetOuterClass.Target target = fusionTargetInfoList.getTarget(i);

            if(target.getFusionTarget().getCore().getPosition().getPosType().equals(FusedOuterClass.FusedPositionType.FusedPositionType_Plot)){
                log.info("\\n-------------Receive new Fusion plot!!---------------");
                //use enable_log_bv5_plot for enable send plot bv5 to bms
                if(ReadConfigFile.enableLogBv5Plot) {
                    log.info("\\n-------------Processing Fusion plot!!---------------");
                    processPlots(indexBV, target, i);
                }
            } else {
                log.info("\n-------------Receive new Fusion track!!---------------", fusionTargetInfoList.getTargetCount());
                processTracks(indexBV, target, i);
            }
        }

    }
    public static void handleDroneTargetFromTCP(String indexBV, Common.Message msg) throws InvalidProtocolBufferException {
        vea.api.drone.TargetOuterClass.TargetList targetList = vea.api.drone.TargetOuterClass.TargetList.parseFrom(msg.getPayload().getValue());
        log.info("\n-------------Receive new Drone track !!--------------- {} Drone track ", targetList.getListCount());
        for (int i = 0; i < targetList.getListCount(); i++) {
            BaseMessageProto.BaseMessage.Builder baseMessage = BaseMessageProto.BaseMessage.newBuilder();
            vea.api.drone.TargetOuterClass.Target target = targetList.getList(i);


                log.info("\n-------------Receive new Drone track!!---------------", target.toString());

//                processTracks(indexBV, drone, i);

        }

    }
    public static void processTracks(String indexBV, TargetOuterClass.Target target, int indexFusion){

            BaseMessageProto.BaseMessage.Builder baseMessage = BaseMessageProto.BaseMessage.newBuilder();
            FusedOuterClass.Fused core = target.getFusionTarget().getCore();

            if(ReadConfigFile.enableLogBv5Target){
                log.info("Target index: " + indexFusion + ", info: ");
                log.info(core.getIdentity());
                log.info(core.getPosition());
                log.info(core.getVelocity());
                log.info(core.getTrack());
                log.info(core.getClassification());
                log.info(core.getTrajectoryState());
            }

            TrackMessageProto.SourceInfo sourceInfo = TrackMessageProto.SourceInfo.newBuilder().setId(indexBV).build();
            PositionAndVelocityProto.GeodeticPosition.Builder geo = PositionAndVelocityProto.GeodeticPosition.newBuilder();
            PositionAndVelocityProto.PolarVelocity.Builder vec = PositionAndVelocityProto.PolarVelocity.newBuilder();

            vec.setSpeed((float) target.getFusionTarget().getCore().getVelocity().getCommon().getSpeed())
                    .setHeading((float) (target.getFusionTarget().getCore().getVelocity().getCommon().getHeading()*Math.PI/180.0)).build();

            if(target.getFusionTarget().getCore().getPosition().getCommon().getPosLlaCount() > 0) {
                geo.setLatitude((float) (target.getFusionTarget().getCore().getPosition().getCommon().getPosLla(0)*Math.PI/180.0))
                        .setLongitude((float) (target.getFusionTarget().getCore().getPosition().getCommon().getPosLla(1)*Math.PI/180.0));
                //Check-Neu la radar 3D moi gui gia tri do cao
                if(target.getFusionTarget().getCore().getPosition().getCommon().getTrackingType() == vea.api.data.target.Common.TRACKING_TYPE.TRACKING_TYPE_3D){
                    geo.setAltitude((float) target.getFusionTarget().getCore().getPosition().getCommon().getPosLla(2)).build();
                }
            }

            TrackInfoProto.AircraftInfo.Builder aircaftInfo = TrackInfoProto.AircraftInfo.newBuilder();
            // Kieu loai may bay
            if (target.getFusionTarget().getCore().getClassification().getType().equals(FUSED_CLASSIFICATION_TYPE_DRONE)) {
                aircaftInfo.setAircraftType(16);   //AIR_CRAFT_TYPE_DRONE        -> refer to TrackInfoEnum.proto
            } else if (target.getFusionTarget().getCore().getClassification().getType().equals(FUSED_CLASSIFICATION_TYPE_UAV_TRINHSAT)) {
                aircaftInfo.setAircraftType(19);   //AIRCRAFT_TYPE_RECON_UAV     -> refer to TrackInfoEnum.proto
            } else if (target.getFusionTarget().getCore().getClassification().getType().equals(FUSED_CLASSIFICATION_TYPE_UAV_TANCONG)) {
                aircaftInfo.setAircraftType(18);   //AIRCRAFT_TYPE_ASSAULT_UAV   -> refer to TrackInfoEnum.proto
            }  else
                aircaftInfo.setAircraftType(0);    //AIRCRAFT_TYPE_UNKNOWN
            TrackInfoProto.TrackInfo.Builder trackInfo = TrackInfoProto.TrackInfo.newBuilder();

            // Trang thai che ap cua muc tieu
            int maneuverStatus = 1;     // default: STATE_NORMAL
            if(target.getFusionTarget().getCore().getTrajectoryState().equals(STATE_LOST_TRAJECTORY)){
                maneuverStatus = 2;     // 2 = LOST_CONTROL
            } else if(target.getFusionTarget().getCore().getTrajectoryState().equals(STATE_LOST_HEIGHT)){
                maneuverStatus = 3;     // 3 = LOST_ALTITUDE
            } else if(target.getFusionTarget().getCore().getTrajectoryState().equals(STATE_LOST_TRAJECTORY_AND_HEIGHT)){
                maneuverStatus = 4;     // 4 = LOST_CONTROL_AND_ALTITUDE
            }
            trackInfo.setAircraftInfo(aircaftInfo)
                    .setManeuverStatus(maneuverStatus);

            if((target.getFusionTarget().getCore().getIdentity().getFeatures().getDoaTargetCount() > 0)
                    && (target.getFusionTarget().getCore().getIdentity().getFeatures().getDoaTarget(0).getIdentity().getFeatures().getMeanFcsCount() > 1)) {
                trackInfo.setStartRadiationFreq(target.getFusionTarget().getCore().getIdentity().getFeatures().getDoaTarget(0).getIdentity().getFeatures().getMeanFcs(0))
                        .setEndRadiationFreq(target.getFusionTarget().getCore().getIdentity().getFeatures().getDoaTarget(0).getIdentity().getFeatures().getMeanFcs(1));
            }

            //add by quyetdd3 for get cancel target msg from bv5
            if(target.getFusionTarget().getCore().getTrack().getIrradType().equals(vea.api.data.target.Common.IRRAD_TYPE.IRRAD_TYPE_DELETE)){
                TrackMessageProto.RemovedTrackEventMessage.Builder removedTrackEventMsg = TrackMessageProto.RemovedTrackEventMessage.newBuilder();
                removedTrackEventMsg.setRemovedTrackId((int)target.getFusionTarget().getCore().getTrack().getTrackNumber())
                        .setSourceInfo(sourceInfo);
                baseMessage.setMessageType(3); // REMOVE_TRACK_MESSAGE  = 3
                baseMessage.setMessageDetail(Any.pack(removedTrackEventMsg.build()));
                log.info("Remove track with id: " + (int)target.getFusionTarget().getCore().getTrack().getTrackNumber()
                        + " - " + baseMessage.toString());
            } else {
                TrackMessageProto.TrackMessage trackMessage = TrackMessageProto.TrackMessage.newBuilder()
                        .setSourceInfo(sourceInfo)
                        .setId((int)target.getFusionTarget().getCore().getTrack().getTrackNumber())
                        .setPolarVelocity(vec)
                        .setTrackInfo(trackInfo.build())
                        .setGeodeticPosition(geo.build()).build();

                if(ReadConfigFile.enableLogBv5Target){
                    log.info(trackMessage);
                }
                baseMessage.setMessageType(1);
                baseMessage.setMessageDetail(Any.pack(trackMessage));
            }
            //end add by quyetdd3 for get cancel target msg from bv5


            ConnectionNatIO.getInstance().publishMessage("bv-connect-bv5.track.*.false.*", baseMessage.build());

    }

    public static void processPlots(String indexBV, TargetOuterClass.Target target, int indexFusion){
        FusedOuterClass.Fused core = target.getFusionTarget().getCore();
        if(ReadConfigFile.enableLogBv5Target){
            log.info("Plot index: " + indexFusion + ", info: ");
            log.info(core.getIdentity());
            log.info(core.getPosition());
            log.info(core.getVelocity());
            log.info(core.getTrack());
            log.info(core.getClassification());
            log.info(core.getTrajectoryState());
        }
        int plotNumber = (int)target.getFusionTarget().getCore().getTrack().getTrackNumber();
        String source = plotNumber + "_" + indexBV;
        if(target.getFusionTarget().getCore().getTrack().getIrradType().equals(vea.api.data.target.Common.IRRAD_TYPE.IRRAD_TYPE_DELETE)){
            log.info("Remove plot with id: " + plotNumber);
//            listCancellationBv5PlotMessage.add(source);
            GrpcChannel.getInstance().deleteBv5Plot(source);
        } else {
            double lat = target.getFusionTarget().getCore().getPosition().getCommon().getPosLla(0);
            double lon = target.getFusionTarget().getCore().getPosition().getCommon().getPosLla(1);

            T24.QdtPoint.Builder qdtPoint = T24.QdtPoint.newBuilder();
            qdtPoint.setQdtSourceId(source).setLongitude(lon).setLatitude(lat);
            //Check-Neu la radar 3D moi gui gia tri do cao
            if(target.getFusionTarget().getCore().getPosition().getCommon().getTrackingType() == vea.api.data.target.Common.TRACKING_TYPE.TRACKING_TYPE_3D){
                qdtPoint.setAltitude(target.getFusionTarget().getCore().getPosition().getCommon().getPosLla(2)).build();
            }
            GrpcChannel.getInstance().createBv5Plot(qdtPoint.build());
        }
    }

}
