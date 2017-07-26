package com.ecm.alfresco.migration.bean.document;


public class DocumentAssociation {
    String srcRepoSourceId;
    String srcRepoTargetId;
    String tarRepoSourceId;
    String tarRepoTargetId;
    String type;
    String status;
    String message;

    public DocumentAssociation(String sourceId, String targetId, String type) {
        this.srcRepoSourceId = sourceId;
        this.srcRepoTargetId = targetId;
        this.type = type;
    }

    public DocumentAssociation(){
        
    }

    public String getSrcRepoSourceId() {
        return srcRepoSourceId;
    }

    public void setSrcRepoSourceId(String srcRepoSourceId) {
        this.srcRepoSourceId = srcRepoSourceId;
    }

    public String getSrcRepoTargetId() {
        return srcRepoTargetId;
    }

    public void setSrcRepoTargetId(String srcRepoTargetId) {
        this.srcRepoTargetId = srcRepoTargetId;
    }

    public String getTarRepoSourceId() {
        return tarRepoSourceId;
    }

    public void setTarRepoSourceId(String tarRepoSourceId) {
        this.tarRepoSourceId = tarRepoSourceId;
    }

    public String getTarRepoTargetId() {
        return tarRepoTargetId;
    }

    public void setTarRepoTargetId(String tarRepoTargetId) {
        this.tarRepoTargetId = tarRepoTargetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
