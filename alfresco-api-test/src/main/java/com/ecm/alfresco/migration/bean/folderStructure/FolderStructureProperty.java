package com.ecm.alfresco.migration.bean.folderStructure;

public class FolderStructureProperty implements FolderStructureItem {
    private String id;
    private String[] propertyList;
    private String dateFormat;
    private int levels;
    private boolean includeLastLevel;

    public FolderStructureProperty(String id, String[] propertyList, String dateFormat, int levels, boolean includeLastLevel){
        this.id = id;
        this.propertyList = propertyList;
        this.dateFormat = dateFormat;
        this.levels = levels;
        this.includeLastLevel = includeLastLevel;
    }

    public boolean isIncludeLastLevelEnabled() {
        return includeLastLevel;
    }

    public void setIncludeLastLevel(boolean includeLastLevel) {
        this.includeLastLevel = includeLastLevel;
    }

    public int getLevels() {
        return levels;
    }

    public void setLevels(int levels) {
        this.levels = levels;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getPropertyList() {
        return propertyList;
    }

    public void setPropertyList(String[] propertyList) {
        this.propertyList = propertyList;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
