package com.vht.connection.Vq;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.vht.bms.messages.BaseMessageProto;
import com.vht.bms.messages.PositionAndVelocityProto;
import com.vht.bms.messages.track.TrackInfoProto;
import com.vht.bms.messages.track.TrackMessageProto;
import com.vht.connection.Interface.ConnectionNatIO;
import io.grpc.ConnectivityState;
import io.nats.client.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log4j2
public class Manager extends Thread{
    public static ConcurrentLinkedQueue<DataRaw> dataRawQueue = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<DataRaw> dataToCollectionQueue = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<String> dataToHeartbeat = new ConcurrentLinkedQueue<>();

    String request = "";
    String buffer = "";
    private static final int TIME_DELAY = 10;
    private static final int TIME_REQUEST = 1 * 1000;
    private static final int CNT_BLIND_DATA = 6;
    private static final String CATEGORY = "category";
    private boolean heartBeat = false;
    private boolean heartBeatOld = true;
    private int cntDataBlind = 0;

    private String m_category;
    TcpClient tcpClient;

    public Manager(TcpClient tcpClient){
        this.tcpClient = tcpClient;
    }

//    @Override
    public void start(){
        Thread processMessageFromVqThread = new Thread(() -> {
            int count = 0;
            while (true){
                try {
                    sleep(TIME_DELAY);
                    while (dataRawQueue.peek()!=null){
                        analysisData(dataRawQueue.poll());
                    }
                    createJsonMsg();
                    if((count % TIME_REQUEST) == 0){
                        count = 0;
//                        ByteString buff = ByteString.copyFromUtf8(request);
                        ByteBuf buff = Unpooled.copiedBuffer(request, StandardCharsets.UTF_8);
                        tcpClient.sendMessage(buff);
//                        if(!dataToHeartbeat.offer(request)){
//                            log.warn( "Offer request is failed!");
//                        }
                        if(cntDataBlind >= CNT_BLIND_DATA) {
                            if (heartBeat) heartBeat = false;
                        }
                        else cntDataBlind++;
                    }
                    count++;
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread processMessageToNatThread = new Thread(() -> {
            while(true){
                try{
                    sleep(10);
                    while (dataToCollectionQueue.peek() != null) {
                        sendData(dataToCollectionQueue.poll());
                    }
                }
                catch (Exception e){
                    log.warn("[ProcessGrpc] thread run exception");
                    log.warn(e.toString());
                    Thread.currentThread().interrupt();
                }
            }
        });

        processMessageFromVqThread.start();
        processMessageToNatThread.start();
    }

    public void sendData(DataRaw input){
        try {
            if (input.getJsonObject() != null) {
                switch (input.getJsonObject().get("category").toString()) {
                    case "DeleteTrack":
                        getDeleteTrackMessage(input.getmDataSourceName(), input.getJsonObject());
                        break;
                    case "Track":
                        convertToTrackMessage(input.getmDataSourceName(), input.getJsonObject());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e){
            log.warn("[ProcessGrpc] sendData failed");
            e.printStackTrace();
        }
    }

    public void analysisData (DataRaw dataRaw) {
        String input = dataRaw.getmData();
        if (input != null) {
            buffer += input;
        }
        if(!buffer.equals("")){
            String[] subStrings = buffer.split("}\n");
            buffer = "";
            for(String s: subStrings){
                s += "}";
                int start = s.indexOf("{");
                int end = s.indexOf("}");
                if(start > -1 && end > -1 && end > start){

                    JSONParser jsonParser = new JSONParser();
                    try {
                        JSONObject jsonObj = (JSONObject) jsonParser.parse(s);
                        log.info("jsonobject " + jsonObj.toString());
                        if(jsonObj.containsKey(CATEGORY)) {
                            String category = jsonObj.get(CATEGORY).toString();
                            if (category != null) {
                                if (category.equals("AcceptConnect") ||
                                        category.equals("RejectConnect") ||
                                        category.equals("HeartBeat")) {
                                    heartBeat = true;
                                    cntDataBlind = 0;
                                } else if (category.equals("DeleteTrack") ||
                                        category.equals("Track")) {
                                    String ss = "Category is " + category;
                                    log.info(ss);
                                    DataRaw dataToSys = new DataRaw(dataRaw.getmDataSourceName(), jsonObj);
                                    if (dataToCollectionQueue.size() > 100) {
                                        dataToCollectionQueue.poll();
                                    }
                                    if (!dataToCollectionQueue.offer(dataToSys)) {
                                        log.warn("Offer json object is failed!");
                                    }
                                }
                            }
                        }
                    } catch (ParseException e) {
                        log.warn("String is not format Json");
                    }
                }
            }
        }
    }

    private void createJsonMsg(){
        if(heartBeatOld != heartBeat){
            heartBeatOld = heartBeat;
            if(heartBeat){
                //create message heartBeat
                request = createHeartBeatMsg();
            }
            else{
                //create message request connect
                request = createRequestConnect();
            }
        }
    }

    private String createHeartBeatMsg(){
        JSONObject obj = new JSONObject();
        obj.put(CATEGORY, "HeartBeat");

        return obj.toString();
    }
    private String createRequestConnect(){
        JSONObject obj = new JSONObject();
        String ip = "127.0.0.1";
        String code = digest(ip.getBytes()).toString();

        obj.put(CATEGORY, "RequestConnect");
        obj.put("code", code);
        return obj.toString();
    }

    private StringBuilder digest(byte[] input){
        MessageDigest md;
        try{
            md = MessageDigest.getInstance("MD5");
        }catch(NoSuchAlgorithmException e){
            throw new IllegalArgumentException(e);
        }
        byte[] result = md.digest(input);
        StringBuilder sb = new StringBuilder();

        for(byte b : result){
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb;
    }

    private void getDeleteTrackMessage(String sourceName, JSONObject lineData) {
        try {
            JSONObject trackJson = lineData;
            String category = trackJson.get("category").toString();
            m_category = category;
            log.info("trackJson:" + trackJson.toString());
            if (category.equals("DeleteTrack")) {
                log.info("delete track with track id " + trackJson.get("track_id").toString());
                try {
                    BaseMessageProto.BaseMessage.Builder baseMessage = BaseMessageProto.BaseMessage.newBuilder();
                    TrackMessageProto.RemovedTrackEventMessage.Builder removedTrackEventMsg = TrackMessageProto.RemovedTrackEventMessage.newBuilder();
                    TrackMessageProto.SourceInfo sourceInfo = TrackMessageProto.SourceInfo.newBuilder().setId("vq").build();
                    removedTrackEventMsg.setRemovedTrackId(Integer.parseInt(trackJson.get("track_id").toString()))
                            .setSourceInfo(sourceInfo);
                    baseMessage.setMessageType(3); // REMOVE_TRACK_MESSAGE  = 3
                    baseMessage.setMessageDetail(Any.pack(removedTrackEventMsg.build()));
                    log.info("Remove track with id: " + trackJson.get("track_id").toString()
                            + " - " + baseMessage.toString());
                    ConnectionNatIO.getInstance().publishMessage("bv-connect-bv5.track.*.false.*", baseMessage.build());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (category.equals("DeleteTrack")) {

            } else {
                log.info("category:" + category + ", trackJson:" + trackJson.toString());
            }
        } catch (Exception e){
            log.info("[Delete Track] Convert to Track Message failed");
            e.printStackTrace();
        }
    }
    private void convertToTrackMessage(String sourceName,JSONObject lineData) {
//        JSONObject trackJson = new JSONObject(lineData);
        try {
            JSONObject trackJson = lineData;
            String category = trackJson.get("category").toString();
            m_category = category;
            log.info("trackJson:" + trackJson.toString());
            if (category.equals("Track")) {
                log.info("receive track with track number " + trackJson.get("track_number").toString());
                try {
                    BaseMessageProto.BaseMessage.Builder baseMessage = BaseMessageProto.BaseMessage.newBuilder();
                    TrackMessageProto.SourceInfo sourceInfo = TrackMessageProto.SourceInfo.newBuilder().setId("VQ").build();
                    PositionAndVelocityProto.PolarVelocity.Builder vec = PositionAndVelocityProto.PolarVelocity.newBuilder();

                    vec.setSpeed((float) (Float.parseFloat(trackJson.get("speed").toString())/3.6))
                            .setHeading((float) (Float.parseFloat(trackJson.get("heading").toString())*Math.PI/180.0)).build();

                    PositionAndVelocityProto.GeodeticPosition.Builder geo = PositionAndVelocityProto.GeodeticPosition.newBuilder();
                        geo.setLatitude((float) (Float.parseFloat(trackJson.get("latitude").toString())*Math.PI/180.0))
                                .setLongitude((float) (Float.parseFloat(trackJson.get("longitude").toString())*Math.PI/180.0));
                        geo.setAltitude(Float.parseFloat(trackJson.get("height").toString())).build();



                // tb/
                    /*
                    // =0 : UNKNOWN
                        =1: ALLY
                        =2: NEUTRAL //HKDD
                        =3: ENEMY
                        =4: THREAT
                        =5: OWN_FORCE // Ta
                    */
                    int convertIdentity = 0;

                    if (trackJson.containsKey("identity")) {

                        int m_identity = Integer.parseInt(trackJson.get("identity").toString());
                        switch (m_identity) {
                            case 0: // NoGroup
                                convertIdentity = 0; //unknown
                                break;
                            case 1: // Friendly
                                convertIdentity = 5; // Friend
                                break;
                            case 2: // Enemy
                                convertIdentity = 3; // Hostile
                                break;
                            case 3: // international
                                convertIdentity = 2; // HKDD
                                break;
                            case 4: //transit
                                convertIdentity = 2; // HKDD
                                break;
                            case 5: //domestic
                                convertIdentity = 2; // HKDD
                                break;
                            default:
                                log.warn("identity invalid " + m_identity);
                        }
                    }
                    TrackInfoProto.TrackInfo.Builder trackInfo = TrackInfoProto.TrackInfo.newBuilder();
                    trackInfo.setFriendFoeState(convertIdentity);
                    //tb\


                    TrackMessageProto.TrackMessage trackMessage = TrackMessageProto.TrackMessage.newBuilder()
                            .setSourceInfo(sourceInfo)
                            .setId(Integer.parseInt(trackJson.get("track_id").toString()))
                            .setPolarVelocity(vec)
                            .setTrackInfo(trackInfo.build())
                            .setGeodeticPosition(geo.build()).build();

                    baseMessage.setMessageType(1);
                    baseMessage.setMessageDetail(Any.pack(trackMessage));

                    ConnectionNatIO.getInstance().publishMessage("bv-connect-bv5.track.*.false.*", baseMessage.build());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (category.equals("DeleteTrack")) {

            } else {
                log.info("category:" + category + ", trackJson:" + trackJson.toString());
            }
        } catch (Exception e){
            log.info("Convert to Track Message failed");
            e.printStackTrace();
        }
    }
}
