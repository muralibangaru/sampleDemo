package com.ecm.alfresco.migration.bean.document;

import org.apache.chemistry.opencmis.client.api.Document;
import org.json.JSONObject;

import com.ecm.alfresco.migration.job.param.JobParameters;

import java.util.ArrayList;
import java.util.List;

public class DocumentItem {
    private Document document;
    private String documentId;
    private String labelVersion;
    private String folderPath;
    private String jsonProperties;
    private String batchID;
    private String sourceNodeRef;
    private String targetNodeRef;
    private String versionNodeRef;
    private String contentType;
    private String versionLabel;
    private String modifiedTimeStamp;
    private String migrationTimeStamp;
    private String successFlag;
    private String status;
    private String associationStatus;
    private String filename;
    private String targetDestinationFolder;
    private String message = "";
    private String exception;
    private int countVersions;
    private DocumentProperties documentProperties;
    private List<DocumentAssociation> associations = new ArrayList<>();
    
    private String creator;
    private String modifier;
    private String created;

    public DocumentItem(String documentId) {
        this.documentId = documentId;
    }

    public DocumentItem(Document document, String folderPath) {
        this.document = document;
        this.documentId = document.getId();
        this.folderPath = folderPath;
    }

    public DocumentItem() {

    }

    public DocumentItem(String sourceNodeRef, String filename, String targetDestinationFolder, String targetNodeRef, String successFlag, String status, String message, JobParameters jobParameters, JSONObject metadata) {
        this.sourceNodeRef = sourceNodeRef;
        this.filename = filename;
        this.targetDestinationFolder = targetDestinationFolder;
        this.targetNodeRef = targetNodeRef;
        this.status = status;
        this.message = message;
        this.successFlag = successFlag;
        this.documentProperties = documentProperties;

        if(jobParameters != null)
            batchID = jobParameters.getBatchId();

        if(metadata != null) {
            modifiedTimeStamp = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}modified");
            contentType = (String) metadata.get("type");
            
            creator = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}creator");
            modifier = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}modifier");
            created = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}created");
        }
    }

    public void setAttributes(String sourceNodeRef, String filename, String targetDestinationFolder, JobParameters jobParameters, JSONObject metadata, DocumentProperties documentProperties) {
        this.sourceNodeRef = sourceNodeRef;
        this.filename = filename;
        this.targetDestinationFolder = targetDestinationFolder;
        this.documentProperties = documentProperties;

        if(jobParameters != null)
            batchID = jobParameters.getBatchId();

        if(metadata != null) {
            modifiedTimeStamp = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}modified");
            contentType = (String) metadata.get("type");
            
            creator = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}creator");
            modifier = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}modifier");
            created = (String) ((JSONObject) metadata.get("properties")).get("{http://www.alfresco.org/model/content/1.0}created");
        }
    }

    public DocumentItem setVersionAttributes(String versionLabel, int countVersions) {
        this.versionLabel = versionLabel;
        this.countVersions = countVersions;
        return this;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getLabelVersion() {
        return labelVersion;
    }

    public void setLabelVersion(String labelVersion) {
        this.labelVersion = labelVersion;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getJsonProperties() {
        return jsonProperties;
    }

    public void setJsonProperties(String jsonProperties) {
        this.jsonProperties = jsonProperties;
    }

    public String getBatchID() {
        return batchID;
    }

    public void setBatchID(String batchID) {
        this.batchID = batchID;
    }

    public String getSourceNodeRef() {
        return sourceNodeRef;
    }

    public void setSourceNodeRef(String sourceNodeRef) {
        this.sourceNodeRef = sourceNodeRef;
    }

    public String getTargetNodeRef() {
        return targetNodeRef;
    }

    public void setTargetNodeRef(String targetNodeRef) {
        this.targetNodeRef = targetNodeRef;
    }

    public String getVersionNodeRef() {
        return versionNodeRef;
    }

    public void setVersionNodeRef(String versionNodeRef) {
        this.versionNodeRef = versionNodeRef;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public String getModifiedTimeStamp() {
        return modifiedTimeStamp;
    }

    public void setModifiedTimeStamp(String modifiedTimeStamp) {
        this.modifiedTimeStamp = modifiedTimeStamp;
    }

    public String getMigrationTimeStamp() {
        return migrationTimeStamp;
    }

    public void setMigrationTimeStamp(String migrationTimeStamp) {
        this.migrationTimeStamp = migrationTimeStamp;
    }

    public String getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(String successFlag) {
        this.successFlag = successFlag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTargetDestinationFolder() {
        return targetDestinationFolder;
    }

    public void setTargetDestinationFolder(String targetDestinationFolder) {
        this.targetDestinationFolder = targetDestinationFolder;
    }

    public int getCountVersions() {
        return countVersions;
    }

    public void setCountVersions(int countVersions) {
        this.countVersions = countVersions;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DocumentProperties getDocumentProperties() {
        return documentProperties;
    }

    public void setDocumentProperties(DocumentProperties documentProperties) {
        this.documentProperties = documentProperties;
    }

    public List<DocumentAssociation> getAssociations() {
        return associations;
    }

    public void setAssociations(List<DocumentAssociation> associations) {
        this.associations = associations;
    }

    public String getAssociationStatus() {
        return associationStatus;
    }

    public void setAssociationStatus(String associationStatus) {
        this.associationStatus = associationStatus;
    }

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}
}

