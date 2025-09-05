package com.vht.connection.Interface;

import com.google.protobuf.ByteString;
import com.vht.connection.BVManager;
import com.vht.connection.Objects.DfArea;
import com.vht.connection.Objects.DfDevice;
import io.grpc.*;
import lombok.extern.log4j.Log4j2;
import org.json.JSONException;
import org.json.JSONObject;
import patch_options.PatchOptionsOuterClass;
import search_options.SearchOptionsOuterClass;
import storage.FileServiceGrpc;
import storage.Storage;
import track.*;
import track.T24.*;

import javax.json.Json;
import javax.json.JsonPatch;

//import static com.vht.connection.ReadConfigFile.empGrpcAddress;
import static com.vht.connection.TCPConnectionHandler.ModuleStatusManager.putModuleStatusInfoToMapDevice;

@Log4j2
public class GrpcChannel {
    private VungCheApServiceGrpc.VungCheApServiceBlockingStub blockingStubThongTinCheAp;
    private ResponseServiceGrpc.ResponseServiceBlockingStub blockingStubResponse;
    private KhiTaiServiceGrpc.KhiTaiServiceBlockingStub blockingStubKhiTai;
    private VungTrinhSatServiceGrpc.VungTrinhSatServiceBlockingStub blockingStubThongTinTrinhSat;
    private TrangThaiKhiTaiServiceGrpc.TrangThaiKhiTaiServiceBlockingStub blockingStubTrangThaiKhiTai;
    private CoverageServiceGrpc.CoverageServiceBlockingStub blockingStubCoverageInfo;
    private FileServiceGrpc.FileServiceBlockingStub blockingStubUploadFile;

    private QdtPointServiceGrpc.QdtPointServiceBlockingStub blockingQdtPointStub;

    private static GrpcChannel grpcInstance;

    public static GrpcChannel getInstance(){
        if(grpcInstance == null) {
            grpcInstance = new GrpcChannel();
        }
        return grpcInstance;
    }

    public void onCreateChannel() {
        ManagedChannel channelEMPInfo = ManagedChannelBuilder.forAddress(empGrpcAddress, 30465).usePlaintext().build();
        ManagedChannel channelVPInfo = ManagedChannelBuilder.forAddress(empGrpcAddress, 30265).usePlaintext().build();

        ClientInterceptor interceptor = new HeaderClientInterceptor();

        Channel channel1 = ClientInterceptors.intercept(channelEMPInfo, interceptor);
        Channel channel2 = ClientInterceptors.intercept(channelVPInfo, interceptor);

        blockingStubThongTinCheAp = VungCheApServiceGrpc.newBlockingStub(channel1);
        blockingStubResponse = ResponseServiceGrpc.newBlockingStub(channel1);
        blockingStubKhiTai = KhiTaiServiceGrpc.newBlockingStub(channel1);
        blockingStubThongTinTrinhSat = VungTrinhSatServiceGrpc.newBlockingStub(channel1);
        blockingStubTrangThaiKhiTai = TrangThaiKhiTaiServiceGrpc.newBlockingStub(channel1);
        blockingStubCoverageInfo = CoverageServiceGrpc.newBlockingStub(channel1);

        blockingQdtPointStub = QdtPointServiceGrpc.newBlockingStub(channel1);

        // Upload file o port khac
        blockingStubUploadFile = FileServiceGrpc.newBlockingStub(channel2);
    }

    public void createNewDfArea(VungTrinhSat dfInfo){
        try {
            VungTrinhSat response = blockingStubThongTinTrinhSat.create(dfInfo);
            log.info(dfInfo.getKhiTaiId() + "|||     Create new vung TRINH SAT VO TUYEN");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String searchVungTrinhSatByIdAtStart(int id, String tramName, String bvId){
        try {
            SearchOptionsOuterClass.SearchOptions request;

            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .build();
            SearchVungTrinhSatResponse resVungTrinhSat =  blockingStubThongTinTrinhSat.search(request);
            boolean cond1 = false;
            boolean cond2 = false;
            boolean cond3 = false;
            if(!resVungTrinhSat.getVungTrinhSatsList().isEmpty()) {
                for(int i=0; i< resVungTrinhSat.getVungTrinhSatsCount(); i++){
                    cond1 = resVungTrinhSat.getVungTrinhSats(i).getIDReQuat() == id;
                    cond2 = resVungTrinhSat.getVungTrinhSats(i).getKhiTaiId().contains(bvId);
                    cond3 = resVungTrinhSat.getVungTrinhSats(i).getTramName().contains(tramName);

                    if( cond1  && cond2
                            &&  cond3 ){
                        long stationSrcId = Long.parseLong(resVungTrinhSat.getVungTrinhSats(i).getTramId());
                        long deviceId = getDeviceIdFromTramName(tramName);

                        if(deviceId != -1){
                            DfArea newDfInfo = new DfArea();
                            newDfInfo.m_idArea = id;
                            newDfInfo.m_azimuth = resVungTrinhSat.getVungTrinhSats(i).getGocPhuongVi();
                            newDfInfo.m_elevation = resVungTrinhSat.getVungTrinhSats(i).getGocTa();
                            newDfInfo.m_fc_begin = resVungTrinhSat.getVungTrinhSats(i).getTanSoTrungTam() - resVungTrinhSat.getVungTrinhSats(i).getBangThong()/2;
                            newDfInfo.m_fc_end = resVungTrinhSat.getVungTrinhSats(i).getTanSoTrungTam() + resVungTrinhSat.getVungTrinhSats(i).getBangThong()/2;

                            DfDevice dfDev = BVManager.getInstance().getBVModuleById(bvId).mapStationContinuous
                                    .get(stationSrcId).mapDfDeviceId.get(deviceId);
                            dfDev.putDfInfoToMapDevice(id, newDfInfo);
                            BVManager.getInstance().getBVModuleById(bvId).mapStationContinuous
                                    .get(stationSrcId).putDeviceToMapStation(deviceId, dfDev);

                            return resVungTrinhSat.getVungTrinhSats(i).getID();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void removeAllDfAreaWhenStart(String bvId) {
        log.info(bvId + "|||     Delete ALL vung TRINH SAT VO TUYEN when start");
        SearchOptionsOuterClass.SearchOptions request;

        request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                .build();
        SearchVungTrinhSatResponse resVungTrinhSat = blockingStubThongTinTrinhSat.search(request);
        String idExistDf = null;
        if (!resVungTrinhSat.getVungTrinhSatsList().isEmpty()) {
            for (int i = 0; i < resVungTrinhSat.getVungTrinhSatsCount(); i++) {
                if (resVungTrinhSat.getVungTrinhSats(i).getKhiTaiId().equals(bvId)) {
                    idExistDf = resVungTrinhSat.getVungTrinhSats(i).getID();
                    SearchOptionsOuterClass.SearchOptions request2;
                    if (idExistDf != null) {
                        SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
                        stringRequest.addStr(idExistDf);

                        request2 = SearchOptionsOuterClass.SearchOptions.newBuilder()
                                .putFilter("id", stringRequest.build())
                                .build();
                        SearchVungTrinhSatResponse resKhiTai = blockingStubThongTinTrinhSat.delete(request2);
                    }
                }
            }
        }
    }

    public long getDeviceIdFromTramName(String tramName){
        try{
            int startCharIdx = tramName.indexOf("[");
            int endCharIdx = tramName.indexOf("]");
            String resStr = tramName.substring(startCharIdx+1, endCharIdx);
            return Long.parseLong(resStr);
        }
        catch (NumberFormatException e){
            return -1;
        }
    }

    public void patchExistDfArea(int id, String bvId, VungTrinhSat dfInfo){
        try {
            log.info(bvId + "|||     Update vung TRINH SAT VO TUYEN");
            SearchOptionsOuterClass.SearchOptions request;

            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .build();
            SearchVungTrinhSatResponse resVungTrinhSat = blockingStubThongTinTrinhSat.search(request);

            String idExistDf = null;

            if(!resVungTrinhSat.getVungTrinhSatsList().isEmpty()) {
                for(int i=0; i< resVungTrinhSat.getVungTrinhSatsCount(); i++){
                    long devId = getDeviceIdFromTramName(resVungTrinhSat.getVungTrinhSats(i).getTramName());
                    if(devId != -1){
                        if(     resVungTrinhSat.getVungTrinhSats(i).getIDReQuat() == id
                                && resVungTrinhSat.getVungTrinhSats(i).getKhiTaiId().equals(bvId)
                                && devId == getDeviceIdFromTramName(dfInfo.getTramName())  ){
                            idExistDf = resVungTrinhSat.getVungTrinhSats(i).getID();
                        }
                    }
                }
                if(idExistDf!=null){
                    PatchOptionsOuterClass.PatchOptions.Builder patchDfInfo = PatchOptionsOuterClass.PatchOptions.newBuilder();
                    String jsonRes = "[" +
                            replaceFieldPatchJson("/status", dfInfo.getStatus()) + "," +
                            replaceFieldPatchJson("/goc_phuong_vi", dfInfo.getGocPhuongVi()) + "," +
                            replaceFieldPatchJson("/goc_ta", dfInfo.getGocTa()) + "," +
                            replaceFieldPatchJson("/tan_so_trung_tam", dfInfo.getTanSoTrungTam()) + "," +
                            replaceFieldPatchJson("/bang_thong", dfInfo.getBangThong()) + "," +
                            replaceFieldPatchJson("/distance", dfInfo.getDistance()) + "," +
                            replaceFieldPatchJson("/angle_start", dfInfo.getAngleStart()) + "," +
                            replaceFieldPatchJson("/angle_end", dfInfo.getAngleEnd())
                            + "]";

                    patchDfInfo.setID(idExistDf).setOperations(ByteString.copyFrom(jsonRes.getBytes()));
                    PatchOptionsOuterClass.PatchResponse patchResponse = blockingStubThongTinTrinhSat.patch(patchDfInfo.build());
//                    log.info(patchResponse.toString());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeExistDf(int id, String tramName, String bvId){
        try {
            log.info(bvId + "|||     Delete vung TRINH SAT VO TUYEN");
            SearchOptionsOuterClass.SearchOptions request1;
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();


            request1 = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .build();
            SearchVungTrinhSatResponse resVungTrinhSat = blockingStubThongTinTrinhSat.search(request1);

            String idExistDf = null;
            if(!resVungTrinhSat.getVungTrinhSatsList().isEmpty()) {
                for(int i=0; i< resVungTrinhSat.getVungTrinhSatsCount(); i++){
                    if(resVungTrinhSat.getVungTrinhSats(i).getIDReQuat() == id
                            && resVungTrinhSat.getVungTrinhSats(i).getKhiTaiId().equals(bvId)
                            && resVungTrinhSat.getVungTrinhSats(i).getTramName().equals(tramName)){
                        idExistDf = resVungTrinhSat.getVungTrinhSats(i).getID();
                        break;
                    }
                }
            }

            SearchOptionsOuterClass.SearchOptions request2;
            if(idExistDf!=null){
                stringRequest.addStr(idExistDf);

                request2 = SearchOptionsOuterClass.SearchOptions.newBuilder()
                        .putFilter("id", stringRequest.build())
                        .build();
                SearchVungTrinhSatResponse resKhiTai = blockingStubThongTinTrinhSat.delete(request2);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }


    public JSONObject replaceFieldPatchJson(String fieldName, Object o){
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("op", "replace");
            jsonResult.put("path", fieldName);
            jsonResult.put("value", o);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonResult;
    }

    public void sendEmpJammer(VungCheAp empInfo) {
        try {
            blockingStubThongTinCheAp.create(empInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createResponse(Response response) {
        try {
            Response res = blockingStubResponse.create(response);
            log.info("send response to Grpc: " +  res);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateStatusBV5(String BV, int status) {
        try {
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
            SearchOptionsOuterClass.SearchOptions request;

            stringRequest.addStr(BV);
            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .putFilter("name", stringRequest.build())
                    .build();
            SearchKhiTaiResponse resKhiTai = blockingStubKhiTai.search(request);

            if(!resKhiTai.getKhiTaisList().isEmpty()) {
                String id = resKhiTai.getKhiTais(0).getID();

                PatchOptionsOuterClass.PatchOptions.Builder patchKhiTai = PatchOptionsOuterClass.PatchOptions.newBuilder();
                JsonPatch controlJsonPatch = Json.createPatchBuilder()
                        .replace("/status", status)
                        .build();
                patchKhiTai.setID(id).setOperations(ByteString.copyFrom(controlJsonPatch.toString().getBytes()));
                PatchOptionsOuterClass.PatchResponse patchResponse = blockingStubKhiTai.patch(patchKhiTai.build());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public String searchForKhiTai(String khiTaiName) {
        try {
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
            SearchOptionsOuterClass.SearchOptions request;

            stringRequest.addStr(khiTaiName);
            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .putFilter("name", stringRequest.build())
                    .build();
            SearchTrangThaiKhiTaiResponse resKhiTai = blockingStubTrangThaiKhiTai.search(request);

            if(!resKhiTai.getTrangThaiKhiTaisList().isEmpty()) {
                String id = resKhiTai.getTrangThaiKhiTais(0).getID();
                return id;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void createNewTrangThaiKhiTai(TrangThaiKhiTai khiTaiStatus){
        try{
            blockingStubTrangThaiKhiTai.create(khiTaiStatus);
            log.info(khiTaiStatus.getName() + "|||       Create new trang thai khi tai thanh cong!!!");
        }catch (Exception e) {
            log.error(khiTaiStatus.getName() + "|||       Failed to create new trang thai khi tai!!!");
            e.printStackTrace();
        }
    }

    public void sendTrangThaiKhiTai(TrangThaiKhiTai khiTaiStatus){
        String idKhiTaiExist = searchForKhiTai(khiTaiStatus.getName());
        if(idKhiTaiExist == null){
            createNewTrangThaiKhiTai(khiTaiStatus);
        }else{
            PatchOptionsOuterClass.PatchOptions.Builder patchKhiTaiStt = PatchOptionsOuterClass.PatchOptions.newBuilder();
            String jsonRes = "[" +
                    replaceFieldPatchJson("/name", khiTaiStatus.getName()) + "," +
                    replaceFieldPatchJson("/type", khiTaiStatus.getType()) + "," +
                    replaceFieldPatchJson("/sscd", khiTaiStatus.getSSCD()) + "," +
                    replaceFieldPatchJson("/che_do_hoat_dong", khiTaiStatus.getCheDoHoatDong()) + "," +
                    replaceFieldPatchJson("/station_altitude", khiTaiStatus.getStationAltitude()) + "," +
                    replaceFieldPatchJson("/station_latitude", khiTaiStatus.getStationLatitude()) + "," +
                    replaceFieldPatchJson("/station_longitude", khiTaiStatus.getStationLongitude()) + "," +
                    replaceFieldPatchJson("/toc_do_quet", khiTaiStatus.getTocDoQuet()) + "," +
                    replaceFieldPatchJson("/cong_suat_phat", khiTaiStatus.getCongSuatPhat()) + "," +
                    replaceFieldPatchJson("/cu_ly_canh_gioi", khiTaiStatus.getCuLyCanhGioi()) + "," +
                    replaceFieldPatchJson("/che_do_anh_nhiet", khiTaiStatus.getCheDoAnhNhiet()) + "," +
                    replaceFieldPatchJson("/muc_zoom_hien_tai", khiTaiStatus.getMucZoomHienTai()) + "," +
                    replaceFieldPatchJson("/ket_qua_do_xa", khiTaiStatus.getKetQuaDoXa()) + "," +
                    replaceFieldPatchJson("/trang_thai_bat_bam_muc_tieu", khiTaiStatus.getTrangThaiBatBamMucTieu())
                    + "]";

            patchKhiTaiStt.setID(idKhiTaiExist).setOperations(ByteString.copyFrom(jsonRes.getBytes()));
            PatchOptionsOuterClass.PatchResponse patchResponse = blockingStubTrangThaiKhiTai.patch(patchKhiTaiStt.build());
            if(patchResponse.getIsOk()){
                log.info(khiTaiStatus.getName() + "|||       Update trang thai khi tai thanh cong!!!");
            } else{
                log.error(khiTaiStatus.getName() + "|||       Failed to update trang thai khi tai!!!");
            }
        }
    }

    public int searchModuleTypeByName(String moduleName){
        try {
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
            SearchOptionsOuterClass.SearchOptions request;

            stringRequest.addStr(moduleName);
            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .putFilter("name", stringRequest.build())
                    .build();
            SearchKhiTaiResponse resKhiTai = blockingStubKhiTai.search(request);

            if(!resKhiTai.getKhiTaisList().isEmpty()) {
                return resKhiTai.getKhiTais(0).getType();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public void clearAllModuleStatusAtStart(){
        SearchOptionsOuterClass.SearchOptions request;

        request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                .build();
        SearchTrangThaiKhiTaiResponse resTTKhiTai = blockingStubTrangThaiKhiTai.search(request);

        for(int i=0; i< resTTKhiTai.getTrangThaiKhiTaisCount(); i++){
            sendEmptyInfo(resTTKhiTai.getTrangThaiKhiTais(i).getName());
        }
    }

    public void sendEmptyInfo(String moduleName){
        T24.TrangThaiKhiTai.Builder moduleSttToSend = T24.TrangThaiKhiTai.newBuilder()
                .setName(moduleName);
        moduleSttToSend.setType(GrpcChannel.getInstance().searchModuleTypeByName(moduleName));
        GrpcChannel.getInstance().sendTrangThaiKhiTai(moduleSttToSend.build());
        putModuleStatusInfoToMapDevice(moduleName, System.currentTimeMillis());
    }

    public boolean checkIfModuleStatusEmpty(String moduleName){
        try {
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
            SearchOptionsOuterClass.SearchOptions request;

            stringRequest.addStr(moduleName);
            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .putFilter("name", stringRequest.build())
                    .build();
            SearchTrangThaiKhiTaiResponse resTTKhiTai = blockingStubTrangThaiKhiTai.search(request);

            TrangThaiKhiTai.Builder emptyInstance = TrangThaiKhiTai.newBuilder()
                    .setName(moduleName)
                    .setType(searchModuleTypeByName(moduleName));

            if(!resTTKhiTai.getTrangThaiKhiTaisList().isEmpty()) {
                TrangThaiKhiTai existInstance = resTTKhiTai.getTrangThaiKhiTais(0);
                emptyInstance.setID(existInstance.getID());
                emptyInstance.setCreatedAt(existInstance.getCreatedAt());
                emptyInstance.setUpdatedAt(existInstance.getUpdatedAt());
                if(existInstance.equals(emptyInstance.build())){
                    return true;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public String searchForExistCoverage(String khiTaiId, int height){
        try {
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
            SearchOptionsOuterClass.SearchOptions request;

            stringRequest.addStr(khiTaiId);
            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .putFilter("khi_tai_id", stringRequest.build())
                    .build();
            SearchCoverageResponse resVungPhu = blockingStubCoverageInfo.search(request);

            for(int i=0; i<resVungPhu.getCoveragesCount(); i++){
                if(resVungPhu.getCoverages(i).getHeight() == height){
                    return resVungPhu.getCoverages(i).getID();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void patchExistCoverage(String id, Coverage coverageInfo){
        PatchOptionsOuterClass.PatchOptions.Builder patchCoverage = PatchOptionsOuterClass.PatchOptions.newBuilder();
        String jsonRes = "[" +
                replaceFieldPatchJson("/detection_geometry_json", coverageInfo.getDetectionGeometryJson()) + "," +
                replaceFieldPatchJson("/center_altitude", coverageInfo.getCenterAltitude()) + "," +
                replaceFieldPatchJson("/center_latitude", coverageInfo.getCenterLatitude()) + "," +
                replaceFieldPatchJson("/center_longitude", coverageInfo.getCenterLongitude())
                + "]";

        patchCoverage.setID(id).setOperations(ByteString.copyFrom(jsonRes.getBytes()));
        PatchOptionsOuterClass.PatchResponse patchResponse = blockingStubCoverageInfo.patch(patchCoverage.build());
        if(patchResponse.getIsOk()){
            log.info(coverageInfo.getKhiTaiId() + "_" + coverageInfo.getHeight()  + "|||     Update vung phu thanh cong!!!");
        } else{
            log.error(coverageInfo.getKhiTaiId() + "_" + coverageInfo.getHeight()  + "|||     Failed to update vung phu!!!");
        }
    }

    public void createNewCoverage(Coverage coverageInfo){
        try{
            blockingStubCoverageInfo.create(coverageInfo);
            log.info(coverageInfo.getKhiTaiId() + "_" + coverageInfo.getHeight()  + "|||     Tao vung phu thanh cong!!!");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendVungPhu(Coverage coverageInfo){
        String idCoverageExist = searchForExistCoverage(coverageInfo.getKhiTaiId(), coverageInfo.getHeight());
        if(idCoverageExist == null){
            createNewCoverage(coverageInfo);
        }
        else{
            patchExistCoverage(idCoverageExist, coverageInfo);
        }
    }

    public String uploadCoverageJsonFile(Storage.FileUploadRequest req){
        Storage.File res = blockingStubUploadFile.upload(req);
        return res.getID();
    }

    // tb1/
    public void createBv5Plot(QdtPoint qdtPoint){
        try {
            QdtPoint res = blockingQdtPointStub.create(qdtPoint);
            log.info("|||     Tao bv5 plot thanh cong!!! with source id " + res.getQdtSourceId());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void deleteBv5Plot(String bv5PlotSrcId){
        try {
            SearchOptionsOuterClass.StringList.Builder stringRequest = SearchOptionsOuterClass.StringList.newBuilder();
            SearchOptionsOuterClass.SearchOptions request;
            stringRequest.addStr(bv5PlotSrcId);
            request = SearchOptionsOuterClass.SearchOptions.newBuilder()
                    .putFilter("qdt_source_id", stringRequest.build())
                    .build();

            SearchQdtPointResponse responseQdtPoint = blockingQdtPointStub.search(request);

            for (int i = 0; i < responseQdtPoint.getQdtPointsCount(); i++) {
                SearchOptionsOuterClass.StringList.Builder stringBv5PlotRequest = SearchOptionsOuterClass.StringList.newBuilder();
                SearchOptionsOuterClass.SearchOptions rmQdtRq;
                stringBv5PlotRequest.addStr(responseQdtPoint.getQdtPoints(i).getID());
                rmQdtRq = SearchOptionsOuterClass.SearchOptions.newBuilder()
                        .putFilter("id", stringBv5PlotRequest.build())
                        .build();
                SearchQdtPointResponse res = blockingQdtPointStub.delete(rmQdtRq);
                log.info("|||     Xoa bv5 plot thanh cong!!! with source id " + res.getQdtPoints(0));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    // tb1\
}
