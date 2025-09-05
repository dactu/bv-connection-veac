package com.vht.connection.Process;

import com.google.protobuf.ByteString;
import com.vht.connection.Interface.GrpcChannel;
import com.vht.connection.Objects.CoverageInfo;
import com.vht.connection.ReadConfigFile;
import lombok.extern.log4j.Log4j;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import storage.Storage;
import track.T24;
import vea.api.Common;
import vea.api.coverage.CoverageOuterClass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static vea.api.coverage.CoverageOuterClass.CoverageStationType.*;


@Log4j
public class CommonService {
    static ArrayList<CoverageInfo> cvInfo = new ArrayList<>();

    public GeodesicData getGeodesicData(double longitude, double latitude, double azimuth, double range){
        return Geodesic.WGS84.Direct(latitude,longitude, azimuth,range); // range in meters
    }

    public static void getCoverageInfoFromBv5(String indexBV, CoverageOuterClass.CoverageList listCoverage){
        ConcurrentMap<String, CoverageOuterClass.Coverage> mapCoverage = new ConcurrentHashMap<>();
        for(int i=0; i<listCoverage.getCoveragesCount(); i++){
            String moduleName = "";
            //Enum Type
            //=1: Trinh sat, = 2: Hoa luc
            int type = 0;

            CoverageOuterClass.Coverage coverage = listCoverage.getCoverages(i);
            if(indexBV.equals(ReadConfigFile.veaName1)){
//                if(coverage.getType().equals(RADAR)){ // set empty, khong nhan tu bv5
//                    type = 1;
//                    moduleName = ReadConfigFile.radarU3kName;
//                } else
                if(coverage.getType().equals(OE)){
                    type = 1;
                    moduleName = ReadConfigFile.bv5Lra01Name;
                } else if(coverage.getType().equals(DF)){
                    type = 1;
                    moduleName = ReadConfigFile.dfCov01Name;
                } else if(coverage.getType().equals(JAMMER_COM)){
                    type = 2;
                    moduleName = ReadConfigFile.jmComCov01Name;
                } else if(coverage.getType().equals(JAMMER_GPS)){
                    type = 2;
                    moduleName = ReadConfigFile.jmGpsCov01Name;
                } else if(coverage.getType().equals(EMP)){
                    type = 2;
                    moduleName = ReadConfigFile.empCovName;
                }
            } else {
//                if(coverage.getType().equals(RADAR)){// set empty, khong nhan tu bv5
//                    type = 1;
//                    moduleName = ReadConfigFile.radarU2xName;
//                } else
                if(coverage.getType().equals(OE)){
                    type = 1;
                    moduleName = ReadConfigFile.bv5Lra02Name;
                } else if(coverage.getType().equals(DF)){
                    type = 1;
                    moduleName = ReadConfigFile.dfCov02Name;
                } else if(coverage.getType().equals(JAMMER_COM)){
                    type = 2;
                    moduleName = ReadConfigFile.jmComCov02Name;
                } else if(coverage.getType().equals(JAMMER_GPS)){
                    type = 2;
                    moduleName = ReadConfigFile.jmGpsCov02Name;
                } else if(coverage.getType().equals(EMP)){
                    type = 2;
                    moduleName = "TSGN_EMP_BV5.02";
                }
            }

            int height = 0;
            switch (coverage.getHeight()){
                case HEIGHT_50M:
                    height = 50;
                    break;
                case HEIGHT_100M:
                    height = 100;
                    break;
                case HEIGHT_200M:
                    height = 200;
                    break;
                case HEIGHT_500M:
                    height = 500;
                    break;
                case HEIGHT_1KM:
                    height = 1000;
                    break;
                case HEIGHT_2KM:
                    height = 2000;
                    break;
                case HEIGHT_3KM:
                    height = 3000;
                    break;
                case HEIGHT_5KM:
                    height = 5000;
                    break;
                default:
                    break;
            }

            String typeTarget = "";
            switch (coverage.getTargetType()){
                case HELICOPTER:
                    typeTarget = "MBQS";
                    break;
                case FIXED_WING_UAV:
                    typeTarget = "UAV";
                    break;
                case QUADCOPTER:
                    typeTarget = "DRONE";
                    break;
                default:
                    break;
            }

            if(!moduleName.isEmpty()){
                String coverageName = moduleName + "/" + height + "/" + type + "/" + typeTarget;

                if(mapCoverage.containsKey(coverageName)){
                    mapCoverage.replace(coverageName, coverage);
                } else{
                    mapCoverage.put(coverageName, coverage);
                }
            }
        }

        for(Map.Entry<String, CoverageOuterClass.Coverage> entry : mapCoverage.entrySet()){
            String coverageName = entry.getKey();
            CoverageOuterClass.Coverage coverageData = entry.getValue();

            String[] parts = coverageName.split("/");

            if(parts.length != 4){
                return;
            }

            String khiTaiId = parts[0];
            int coverageHeight = 0;
            try{
                coverageHeight = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e){
                log.error(e.getMessage());
            }

            int coverageType = 0;
            try{
                coverageType = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e){
                log.error(e.getMessage());
            }

            String coverageTargetType = parts[3];

            coverageName = khiTaiId + "_" + coverageTargetType + "_" + coverageHeight + ".geojson.json";

            //
            CoverageInfo coverageInfo = new CoverageInfo();
            coverageInfo.setFileName(coverageName);
            coverageInfo.setGroupName(indexBV);
            coverageInfo.setModuleName(khiTaiId+"_"+coverageTargetType);
            coverageInfo.setHeight(coverageHeight);
            coverageInfo.setTypeTarget(coverageTargetType);
            coverageInfo.setCoverageData(coverageData);
            coverageInfo.setTypeCoverage(coverageType);
            cvInfo.add(coverageInfo);
        }
        arrangeCoverage(cvInfo);

        for(CoverageInfo covInfo : cvInfo){
            Storage.FileUploadRequest fileUploadRequest = Storage.FileUploadRequest.newBuilder()
                    .setFileName(covInfo.getFileName())
                    .setContentType("text/plain; charset=utf-8")
                    .setSize(getGeoJsonByteArr(covInfo.getCoverageData().getVertexList(), "BV5").size())
                    .setData(getGeoJsonByteArr(covInfo.getCoverageData().getVertexList(), "BV5"))
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
    }
    public static ByteString getGeoJsonByteArr(List<Common.Location> location, String coverageSource){
        JSONObject geoJson = new JSONObject();
        try {
            Common.Location.Builder test1 = Common.Location.newBuilder();
            test1.setLatitude(location.get(0).getLatitude())
                    .setLongitude(location.get(0).getLongitude())
                    .setAltitude(location.get(0).getAltitude());

            JSONArray features = new JSONArray();
            JSONObject feature = new JSONObject();
            JSONObject properties = new JSONObject();

            JSONObject geometry = new JSONObject();
            JSONArray coordinates = new JSONArray();
            JSONArray coordinate = new JSONArray();
            int size = location.size();
            for(int i = size-1;i>0; i--) {
                JSONArray points = new JSONArray();
                points.put(location.get(i).getLongitude());
                points.put(location.get(i).getLatitude());
                coordinate.put(points);
            }
            // diemr cuoi
            JSONArray points = new JSONArray();
            points.put(test1.getLongitude());
            points.put(test1.getLatitude());
            coordinate.put(points);

            coordinates.put(coordinate);
            geometry.put("coordinates", coordinates);
            geometry.put("type", "Polygon");

            feature.put("properties",properties);
            feature.put("geometry", geometry);
            feature.put("type", "Feature");
            features.put(feature);
            geoJson.put("features", features);
            geoJson.put("type", "FeatureCollection");
            log.info(geoJson.toString());
            return ByteString.copyFromUtf8(geoJson.toString());
        } catch (JSONException e) {
            log.error(e.getMessage());
            return null;
        }
    }


    public static void arrangeCoverage(ArrayList<CoverageInfo> listCvInfo){
        Collections.sort(listCvInfo, new Comparator<CoverageInfo>() {
            @Override
            public int compare(CoverageInfo coverageInfo, CoverageInfo t1) {
                return Integer.compare(coverageInfo.getHeight(), t1.getHeight());
            }
        });


    }
}


