package com.vht.connection.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DfDevice {
    public ConcurrentMap<Long, DfArea> mapDfArea = new ConcurrentHashMap<>();

    public void putDfInfoToMapDevice(long dfId, DfArea dfArea) {
        if(mapDfArea.containsKey(dfId)){
            mapDfArea.replace(dfId, dfArea);
        } else{
            mapDfArea.put(dfId, dfArea);
        }
    }
}
