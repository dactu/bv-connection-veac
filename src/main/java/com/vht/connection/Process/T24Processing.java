package com.vht.connection.Process;

import com.google.protobuf.Any;
import com.vht.bms.messages.BaseMessageProto;
import com.vht.bms.messages.track.TrackMessageProto;
import com.vht.connection.Asterix.AsterixCat48Encoder;
import com.vht.connection.BVManager;
import com.vht.connection.Interface.ConnectionNatIO;
import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.BVModule;
import com.vht.connection.ReadConfigFile;
import io.nats.client.Message;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.log4j.Log4j2;
import net.sf.geographiclib.GeodesicData;
import org.json.JSONObject;
import track.T24;
import vea.api.Common;
import vea.api.command.AssignTarget;

import java.util.concurrent.*;

import static com.vht.connection.ReadConfigFile.*;
import static com.vht.connection.heartbeat.PriorityManager.enableSend;

@Log4j2
public class T24Processing {
    public static final int MAX_SIZE = 1000;
    private static final BlockingQueue<Message> queueMessageNatIO = new ArrayBlockingQueue<>(MAX_SIZE);

    private CommonService commonService = new CommonService();
    private static T24Processing t24Processing = null;

    private static int currentCmdId = 0;

    public ConcurrentMap<Integer, String> mapCommand = new ConcurrentHashMap<>();

    public static T24Processing getInstance() {
        if (t24Processing == null) {
            t24Processing = new T24Processing();
        }
        return t24Processing;
    }

    public void receivedMessageFromNatsIO(Message msg) {
        try {
            queueMessageNatIO.put(msg);
        } catch (InterruptedException e) {
            log.info(e.getMessage());
        }
    }

    public void processMessageFromNatsIO() {
        Thread processMessageFromNatsIOThread = new Thread(() -> {
            log.info("Thread process Nats IO Message");
            while (true) {
                try {
                    TimeUnit.MICROSECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!queueMessageNatIO.isEmpty()) {
                    Message msg = queueMessageNatIO.poll();
                    try {
                        if(msg.getSubject().split("\\.")[0].equals("bv-tactical") && msg.getSubject().split("\\.")[3].equals("false")) {
                            if(msg.getSubject().split("\\.")[2].equals("create"))
                                processCommandAssign(msg.getData());
                            if(msg.getSubject().split("\\.")[2].equals("patch"))
                                processCommandAssign(msg.getData());
                            if(msg.getSubject().split("\\.")[1].equals("khi-tais")){
                                processConnectionEnableStatus(msg.getData());
                            }
                        }
                        else if(msg.getSubject().split("\\.")[0].equals("bv-ipcserver")) {
                            processSendTrack2BV(msg.getData());
                        }
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                }
            }
        });
        processMessageFromNatsIOThread.start();
    }

    public void initT24Processing() {
        ConnectionNatIO.getInstance().initConnection(ReadConfigFile.empGrpcAddress);
        GrpcChannel.getInstance().onCreateChannel();
        processMessageFromNatsIO();
    }

    public void processCommandAssign(byte[] data) {
        try {
            JSONObject dataJson = ConvertByte2Json(data);
            log.info(dataJson);
            BVModule bvModule = BVManager.getInstance().getBVModuleById(dataJson.getString("khi_tai_name"));

            int type = dataJson.getInt("type");
            int status = dataJson.getInt("status");

            if(status == 1 || status == 6) {
                if(bvModule==null) {
                    log.error("No have connection with khi_tai: " + dataJson.getString("khi_tai_name"));
                    T24.Response.Builder res = T24.Response.newBuilder();
                    res.setCommandId(dataJson.getString("id")).setType(5).setNote("No have connection with khi_tai: " + dataJson.getString("khi_tai_name"));
                    GrpcChannel.getInstance().createResponse(res.build());
                    return;
                }

                if(!bvModule.userAuthenStatus || !bvModule.connectionStatus) {
                    log.error("Has not been authenticated | connected " + dataJson.getString("khi_tai_name"));
                    T24.Response.Builder res = T24.Response.newBuilder();
                    res.setCommandId(dataJson.getString("id")).setType(5).setNote("Has not been authenticated or connected");
                    GrpcChannel.getInstance().createResponse(res.build());
                    return;
                }
            }

            AssignTarget.CommandList.Builder assignTargetBuilder = AssignTarget.CommandList.newBuilder();

            if(status == 1) {
                mapCommand.put(currentCmdId, dataJson.getString("id"));
                if (type == 1) { // sucsao
                    double stationLat = dataJson.getDouble("khi_tai_latitude");
                    double stationLon = dataJson.getDouble("khi_tai_longitude");

                    double startAngleSS = dataJson.getDouble("angle_start");
                    double endAngleSS = dataJson.getDouble("angle_end");

                    GeodesicData geodesicStart = commonService.getGeodesicData(stationLon, stationLat, startAngleSS, 3000);
                    GeodesicData geodesicEnd = commonService.getGeodesicData(stationLon, stationLat, endAngleSS, 3000);

                    assignTargetBuilder.addCommand(
                            AssignTarget.Command.newBuilder()
                                    .setCmdId(currentCmdId++)
                                    .setCmdType(AssignTarget.AssignTargetType.AT_SS)
                                    .setStartPos(vea.api.data.target.Common.Position.newBuilder()
                                            .addPosLla(geodesicStart.lat2).addPosLla(geodesicStart.lon2).addPosLla(0).build())
                                    .setEndPos(vea.api.data.target.Common.Position.newBuilder()
                                            .addPosLla(geodesicEnd.lat2).addPosLla(geodesicEnd.lon2).addPosLla(0).build())
                                    .build());
                } else if (type == 2) {
//                    int trackNumber = dataJson.getInt("track_number");
                    int trackNumber = dataJson.getInt("track_number_khi_tai"); // add by quyetdd3
                    double trackLatitude = dataJson.getDouble("track_latitude");
                    double trackLongitude = dataJson.getDouble("track_longitude");

                    assignTargetBuilder.addCommand(
                            AssignTarget.Command.newBuilder()
                                    .setCmdId(currentCmdId++)
                                    .setCmdType(AssignTarget.AssignTargetType.AT_BS)
                                    .setTargetId(vea.api.data.target.Common.Identity.newBuilder()
                                            .setTargNum(trackNumber).build())
                                    .setStartPos(vea.api.data.target.Common.Position.newBuilder()
                                            .addPosLla(trackLatitude).addPosLla(trackLongitude).addPosLla(0).build())
                                    .setEndPos(vea.api.data.target.Common.Position.newBuilder()
                                            .addPosLla(trackLatitude).addPosLla(trackLongitude).addPosLla(0).build())
                                    .build());
                } else if (type == 3) {
//                    int trackNumber = dataJson.getInt("track_number");
                    int trackNumber = dataJson.getInt("track_number_khi_tai"); // add by quyetdd3
                    double trackLatitude = dataJson.getDouble("track_latitude");
                    double trackLongitude = dataJson.getDouble("track_longitude");

                    assignTargetBuilder.addCommand(
                            AssignTarget.Command.newBuilder()
                                    .setCmdId(currentCmdId++)
                                    .setCmdType(AssignTarget.AssignTargetType.AT_Jamming)
                                    .setTargetId(vea.api.data.target.Common.Identity.newBuilder()
                                            .setTargNum(trackNumber).build())
                                    .setStartPos(vea.api.data.target.Common.Position.newBuilder()
                                            .addPosLla(trackLatitude).addPosLla(trackLongitude).addPosLla(0).build())
                                    .setEndPos(vea.api.data.target.Common.Position.newBuilder()
                                            .addPosLla(trackLatitude).addPosLla(trackLongitude).addPosLla(0).build())
                                    .build());
                }
            }
            else if(status == 6) { // Cancel Command
                log.info("Huy command");
                mapCommand.clear();

            }

            if(status == 6 || status == 1) {
                if(enableLogCommand){
                    log.info(assignTargetBuilder);
                }

                Common.Message.Builder cmdBuilder =  Common.Message.newBuilder()
                        .setType(Common.Type.TYPE_COMMAND)
                        .setSubtype(0)
                        .setPayload(Any.pack(assignTargetBuilder.build()))
                        .setTransaction(Common.Transaction.newBuilder().setTimeout(2000).build());
                bvModule.commandQueue.put(cmdBuilder.build());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    void processSendTrack2BV (byte[] data) {
        try {
            BaseMessageProto.BaseMessage baseMessage = BaseMessageProto.BaseMessage.parseFrom(data);
            if(baseMessage.getMessageType() == 1) {
                TrackMessageProto.TrackMessage trackMessage = baseMessage.getMessageDetail().unpack(TrackMessageProto.TrackMessage.class);
                for (BVModule bv : BVManager.getInstance().mapConnection.values()) {

                    boolean checkTrackIn = false;

                    for (TrackMessageProto.TrackMessage sourceTrack : trackMessage.getSourceTracksList()) {
                        if (sourceTrack.getSourceInfo().getId().equals(bv.moduleId)) {
                            checkTrackIn = true;
                            break;
                        }
                    }

                    if(!checkTrackIn) {
                        if(enableLogBmsTarget){
                            log.info(trackMessage);
                        }
                        double range = CalculatePosition.getInstance().calculateDistance(
                                bmsLat * Math.PI / 180.0,
                                bmsLong * Math.PI / 180.0,
                                trackMessage.getGeodeticPosition().getLatitude(),
                                trackMessage.getGeodeticPosition().getLongitude());

                        double phi = CalculatePosition.getInstance().calculateBearing(
                                bmsLat * Math.PI / 180.0,
                                bmsLong * Math.PI / 180.0,
                                trackMessage.getGeodeticPosition().getLatitude(),
                                trackMessage.getGeodeticPosition().getLongitude());

                        double checkDis = CalculatePosition.getInstance().calculateDistance(
                                bv.latitude * Math.PI / 180.0,
                                bv.longitude * Math.PI / 180.0,
                                trackMessage.getGeodeticPosition().getLatitude(),
                                trackMessage.getGeodeticPosition().getLongitude());

                        double altitude = trackMessage.getGeodeticPosition().getAltitude();

                        // tb/
                    /*
                    // =0 : UNKNOWN
                        =1: ALLY
                        =2: NEUTRAL //HKDD
                        =3: ENEMY
                        =4: THREAT
                        =5: OWN_FORCE // Ta
                    */
                        int targetType = 0;
                        targetType = trackMessage.getTrackInfo().getFriendFoeState();
                        // tb\

                        if(enableLogBmsTarget){
                            log.info("check valid before send target to BV5 with track_id " + trackMessage.getId()
                                        + " distance " + checkDis + " enable " + enableSend + " type target " + targetType);
                        }
                        // targetType =3 ~ Enemy
                        if ((checkDis < rangeSendTarget) && (enableSend) && (targetType==3)) {    // change by quyetdd3
                            if(enableLogBmsTarget){
                                log.info("sending to bv5");
                            }
                            bv.getUdpClient().sendMsg(AsterixCat48Encoder.getInstance().encodeAsterixCat48(trackMessage.getId(),
                                    phi * 180.0 / Math.PI,
                                    range,
                                    trackMessage.getPolarVelocity().getSpeed(),
                                    trackMessage.getPolarVelocity().getHeading() * 180.0 / Math.PI, altitude));
                            if(enableLogBmsTarget){
                                log.info("send to bv5 success");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processConnectionEnableStatus(byte[] data) {
        try {
            JSONObject dataJson = ConvertByte2Json(data);
            log.info(dataJson);
            BVModule bvModule = BVManager.getInstance().getBVModuleById(dataJson.getString("name"));

            bvModule.enableConnect = dataJson.getBoolean("connect_enable");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void ResponseCommandBV5(Common.Message msg) {
        AssignTarget.CommandList cmd = null;
        try {
            cmd = AssignTarget.CommandList.parseFrom(msg.getPayload().getValue());
            log.info("response cmd: " + cmd);
            if(cmd.getCommandCount() > 0) {
                int cmdId = cmd.getCommand(0).getCmdId();
                String cmdIdStr = mapCommand.get(cmdId);
                AssignTarget.ACK ack = cmd.getCommand(0).getAck();
                int typeStatus = 0;

                switch (ack) {
                    case ACK_CANCEL:
                        typeStatus = 7;
                        mapCommand.remove(cmdId);
                        break;
                    case ACK_NOTDO:
                        typeStatus = 2;
                        break;
                    case ACK_DOING:
                        typeStatus = 3;
                        break;
                    case ACK_DONE:
                        typeStatus = 4;
                        mapCommand.remove(cmdId);
                        break;
                    default:
                        break;
                }


                T24.Response.Builder res = T24.Response.newBuilder();
                res.setCommandId(cmdIdStr).setType(typeStatus);
                GrpcChannel.getInstance().createResponse(res.build());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    public JSONObject ConvertByte2Json(byte[] data) {
        try {
            JSONObject object = new JSONObject(new String(data));
            byte[] decode  = DatatypeConverter.parseBase64Binary(object.getString("response"));
            return new JSONObject(new String(decode));
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
