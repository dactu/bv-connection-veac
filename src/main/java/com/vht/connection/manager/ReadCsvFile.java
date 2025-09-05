package com.vht.connection.manager;

import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.CoverageInfo;
import com.vht.connection.Process.CommonService;
import com.vht.connection.ReadConfigFile;
import lombok.extern.log4j.Log4j;
import net.sf.geographiclib.GeodesicData;
import storage.Storage;
import track.T24;
import vea.api.Common;
import vea.api.coverage.CoverageOuterClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.vht.connection.Process.CommonService.getGeoJsonByteArr;

@Log4j
public class ReadCsvFile {
    public static boolean finishReadExcelFile = false;
    public static void extractGeoJsonData(String folderPath){
        File parentFolder = new File(folderPath);
        File[] radarSubFolders = parentFolder.listFiles((dir, name) -> name.equals("MRS") || name.equals("SRS") || name.equals("U3K") || name.equals("U2X") || name.equals("LRA"));
        ArrayList<CoverageInfo> coverageList = new ArrayList<>();

        if(radarSubFolders != null){
            for(File subFolder : radarSubFolders){
                File[] csvFiles = subFolder.listFiles((dir, name) -> name.endsWith(".csv"));

                if(csvFiles != null){
                    for(File csvFile : csvFiles){
                        CoverageInfo coverageIdx = parseCsvFile(csvFile);
                        if(coverageIdx != null){
                            coverageList.add(coverageIdx);
                        }
                    }
                }
            }
        }
        CommonService.arrangeCoverage(coverageList);
        for(CoverageInfo covInfo : coverageList){
            Storage.FileUploadRequest fileUploadRequest = Storage.FileUploadRequest.newBuilder()
                    .setFileName(covInfo.getFileName())
                    .setContentType("text/plain; charset=utf-8")
                    .setSize(getGeoJsonByteArr(covInfo.getCoverageData().getVertexList(), "BV").size())
                    .setData(getGeoJsonByteArr(covInfo.getCoverageData().getVertexList(), "BV"))
                    .build();
            String jsonFileId = GrpcChannel.getInstance().uploadCoverageJsonFile(fileUploadRequest);

            T24.Coverage.Builder coverageBuilder = T24.Coverage.newBuilder()
                    .setKhiTaiId(covInfo.getModuleName())
                    .setType(covInfo.getTypeCoverage())
                    .setHeight(covInfo.getHeight())
                    .setDetectionGeometryJson(jsonFileId)
                    .setCenterAltitude(covInfo.getCoverageData().getStationLocation().getAltitude())
                    .setCenterLatitude(covInfo.getCoverageData().getStationLocation().getLatitude())
                    .setCenterLongitude(covInfo.getCoverageData().getStationLocation().getLongitude());

            GrpcChannel.getInstance().sendVungPhu(coverageBuilder.build());
        }
        finishReadExcelFile = true;
    }

    private static CoverageInfo parseCsvFile(File csvFile){
        String fileName = csvFile.getName();
        String[] parts = fileName.split("_");

        if(parts.length != 5){
            return null;
        }

        //add new 10082024
        String deviceName = parts[0];
        double deviceLat, deviceLon;
        if(deviceName.contains("MRS")){
            deviceName = ReadConfigFile.radarMrsName;
            deviceLat = ReadConfigFile.mrsPosLat; // tb
            deviceLon = ReadConfigFile.mrsPosLon; // tb
        } else if(deviceName.contains("SRS01")){
            deviceName = ReadConfigFile.radarSrs01Name;
            deviceLat = ReadConfigFile.srs01PosLat;
            deviceLon = ReadConfigFile.srs01PosLon;
        } else if(deviceName.contains("SRS02")){
            deviceName = ReadConfigFile.radarSrs02Name;
            deviceLat = ReadConfigFile.srs02PosLat;
            deviceLon = ReadConfigFile.srs02PosLon;
        } else if(deviceName.contains("SRS03")){
            deviceName = ReadConfigFile.radarSrs03Name;
            deviceLat = ReadConfigFile.srs03PosLat;
            deviceLon = ReadConfigFile.srs03PosLon;
        } else if (deviceName.contains("U2X")) {
            deviceName = ReadConfigFile.radarU2xName;
            deviceLat = ReadConfigFile.u2xPosLat;
            deviceLon = ReadConfigFile.u2xPosLon;
        } else if (deviceName.contains("U3K")) {
            deviceName = ReadConfigFile.radarU3kName;
            deviceLat = ReadConfigFile.u3kPosLat;
            deviceLon = ReadConfigFile.u3kPosLon;
        } else if (deviceName.contains("LRA01")) {
            deviceName = ReadConfigFile.srsLra01Name;
            deviceLat = ReadConfigFile.lra01PosLat;
            deviceLon = ReadConfigFile.lra01PosLon;
        } else if (deviceName.contains("LRA02")) {
            deviceName = ReadConfigFile.srsLra02Name;
            deviceLat = ReadConfigFile.lra02PosLat;
            deviceLon = ReadConfigFile.lra02PosLon;
        } else if (deviceName.contains("LRA03")) {
            deviceName = ReadConfigFile.srsLra03Name;
            deviceLat = ReadConfigFile.lra03PosLat;
            deviceLon = ReadConfigFile.lra03PosLon;
        } else {
            log.warn("name coverage invalid");
            deviceLat = ReadConfigFile.bmsLat;
            deviceLon = ReadConfigFile.bmsLong;
        }

        String typeOfTrack = parts[1];
        switch (typeOfTrack){
            case "M96":
                typeOfTrack = "UAV";
                break;
            case "TrucThang":
                typeOfTrack = "MBQS";
                break;
            case "Drone":
                typeOfTrack = "DRONE";
                break;
            default:
                break;
        }
        // and add
        int coverageHeight = Integer.parseInt(parts[2].substring(0, parts[2].length()-1));

        if(typeOfTrack.isEmpty())
            return null;

        String coverageName = deviceName + "_" + typeOfTrack + "_" + coverageHeight + ".geojson.json";
        CoverageInfo coverageInfo = new CoverageInfo();
        coverageInfo.setFileName(coverageName);
        coverageInfo.setGroupName(deviceName);
        coverageInfo.setModuleName(deviceName + "_" + typeOfTrack);
        coverageInfo.setHeight(coverageHeight);
        coverageInfo.setTypeTarget(typeOfTrack);
        coverageInfo.setTypeCoverage(1);

        CoverageOuterClass.Coverage.Builder coverageData = CoverageOuterClass.Coverage.newBuilder()
                .setStationLocation(Common.Location.newBuilder()
                        .setLatitude(deviceLat)
                        .setLongitude(deviceLon)
                        .build());
        try(BufferedReader br = new BufferedReader(new FileReader(csvFile))){
            String line;
            while ((line = br.readLine()) != null){
                String[] values = line.split(",");
                if(values.length == 2){
                    double angle = Double.parseDouble(values[0].trim());
//                    double distance = Double.parseDouble(values[1].trim()) * 1000;
                    double distance = Double.parseDouble(values[1].trim());
                    CommonService commonService = new CommonService();
                    GeodesicData geodesicData = commonService.getGeodesicData(deviceLon, deviceLat, angle, distance);
                    coverageData.addVertex(Common.Location.newBuilder()
                                    .setLatitude(geodesicData.lat2)
                                    .setLongitude(geodesicData.lon2)
                                    .build());
                }
            }
        }catch (IOException e){
            log.error(e.getMessage());
            return null;
        }
        coverageInfo.setCoverageData(coverageData.build());
        return coverageInfo;
    }


}
