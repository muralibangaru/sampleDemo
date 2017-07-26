package com.ecm.alfresco.migration.bean.folderStructure;

public class FolderStructureDate implements FolderStructureItem {
    private String id;
    private String dateFormat;
    private int levels;

    public FolderStructureDate(String dateID, String dateFormat, int levels) {
        id = dateID;
        this.dateFormat = dateFormat;
        this.levels = levels;
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

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
