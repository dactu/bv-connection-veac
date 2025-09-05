package com.vht.connection.manager;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.vht.connection.BVManager;
import com.vht.connection.Process.CommonService;
import lombok.extern.log4j.Log4j2;
import vea.api.Common;
import vea.api.coverage.CoverageOuterClass;

import java.util.concurrent.TimeUnit;

@Log4j2
public class CoverageManage extends Thread {
    String bv5Id;

    public CoverageManage(String bv5Id){
        this.bv5Id = bv5Id;
    }

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(!ReadCsvFile.finishReadExcelFile){
                ReadCsvFile.extractGeoJsonData("data");
            }

            if(BVManager.getInstance().mapConnection.containsKey(bv5Id)){
                if (!BVManager.getInstance().getBVModuleById(bv5Id).userAuthenStatus)
                    continue;
                if(!BVManager.getInstance().getBVModuleById(bv5Id).receiveCoverageStatus){
                    sendQueryCoverageInfo();
                }
                else{
                    break;
                }
            }
        }
    }

    public static void handleCoverageData(String indexBV, Common.Message msg) throws InvalidProtocolBufferException {
        BVManager.getInstance().getBVModuleById(indexBV).receiveCoverageStatus = true;
        CoverageOuterClass.CoverageList coverageList = CoverageOuterClass.CoverageList.parseFrom(msg.getPayload().getValue());
        log.info(coverageList.toString());
        CommonService.getCoverageInfoFromBv5(indexBV, coverageList);
    }


    public void sendQueryCoverageInfo() {
        BVManager.getInstance().getBVModuleById(bv5Id).handleTCPConnection.sendData(bv5Id, getQueryCoverageRequest());
    }

    public Common.Message getQueryCoverageRequest(){
        Common.Transaction transactionData = Common.Transaction.newBuilder()
                .setId(InitConnection.currentSendMessageId++)
                .setTimeout(3000)
                .build();

        CoverageOuterClass.QueryAll queryAll = CoverageOuterClass.QueryAll.newBuilder()
                .setTransaction(transactionData)
                .build();

        return Common.Message.newBuilder()
                .setType(Common.Type.TYPE_COVERAGE)
                .setPayload(Any.pack(queryAll))
                .setTransaction(transactionData)
                .build();
    }


}
