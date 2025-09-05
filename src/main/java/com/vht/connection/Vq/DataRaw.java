package com.vht.connection.Vq;

import org.json.simple.JSONObject;

//import org.json.JSONObject;

public class DataRaw {

    public DataRaw(String mDataSourceName2,
                   String mData2) {
        mDataSourceName = mDataSourceName2;
        mData = mData2;
    }
    public DataRaw(String mDataSourceName3, JSONObject jsonData){
        jsonObject = jsonData;
        mDataSourceName = mDataSourceName3;
    }
    public JSONObject jsonObject;
    public String mDataSourceName;
    public String mData;

    public String getmDataSourceName() {
        return mDataSourceName;
    }

    public void setmDataSourceName(String mDataSourceName) {
        this.mDataSourceName = mDataSourceName;
    }

    public String getmData() {
        return mData;
    }

    public void setmData(String mData) {
        this.mData = mData;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }
}