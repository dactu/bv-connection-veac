package com.vht.connection.Objects;

import lombok.Data;
import vea.api.coverage.CoverageOuterClass;

import java.util.ArrayList;
import java.util.List;

@Data
public class CoverageInfo {
    String fileName;
    int typeCoverage;

    public int getTypeCoverage() {
        return typeCoverage;
    }

    public void setTypeCoverage(int typeCoverage) {
        this.typeCoverage = typeCoverage;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    String groupName;
    String moduleName;
    int height;
    String typeTarget;
    ArrayList<CoverageOuterClass.Coverage> listCoverage = new ArrayList<>();
    CoverageOuterClass.Coverage coverageData;

    public CoverageOuterClass.Coverage getCoverageData() {
        return coverageData;
    }

    public void setCoverageData(CoverageOuterClass.Coverage coverageData) {
        this.coverageData = coverageData;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getTypeTarget() {
        return typeTarget;
    }

    public void setTypeTarget(String typeTarget) {
        this.typeTarget = typeTarget;
    }

    public ArrayList<CoverageOuterClass.Coverage> getListCoverage() {
        return listCoverage;
    }

    public void setListCoverage(ArrayList<CoverageOuterClass.Coverage> listCoverage) {
        this.listCoverage = listCoverage;
    }
}
