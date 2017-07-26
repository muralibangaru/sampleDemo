package com.ecm.alfresco.migration.job.writer;

import com.ecm.alfresco.migration.bean.document.DocumentAssociation;
import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.bean.document.DocumentProperties;
import com.ecm.alfresco.migration.job.config.MigrationProperties;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.service.AlfrescoAPIService;
import com.ecm.alfresco.migration.service.DocumentService;

import com.ecm.alfresco.migration.util.*;

import org.apache.chemistry.opencmis.client.api.*;

import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.lang3.time.StopWatch;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class DocumentWriter implements ItemWriter<DocumentItem> {
    private static final String SUCCESS = "SUCCESS";
    private static final String SKIPPED = "SKIPPED";
    private static final String REPLACE = "REPLACE";
    private static final String ERROR = "ERROR";
    private static final String FAILED = "FAILED";
    private static final String MOVE = "MOVE";
    private static final String DELETE = "DELETE";
    private static final String KEEP = "KEEP";
    private static final String WORKSPACE = "workspace://SpacesStore/";

    private static final String PROPERTY_NAME = "{http://www.alfresco.org/model/content/1.0}name";
    private static final String FOLDER_DESTINATION = "FOLDER DESTINATION";

    @Autowired
    private DocumentService documentService;
    @Autowired
    private AlfrescoAPIService alfrescoAPIService;
    @Autowired
    private JobParameters jobParameters;

    private static final Logger logger = Logger.getLogger(DocumentWriter.class);

    /**
     * Migrates documents into the target repository
     * @param documentItemList
     * @throws Exception
     */
    @Override
    public void write(List<? extends DocumentItem> documentItemList) throws Exception {
        for (DocumentItem documentItem : documentItemList) {

            if (documentItem != null) {
                try {
                    StopWatch stopwatch = TimeUtil.startStopWatch();
                    setDocumentItemAttributes(documentItem);
                    skipReplaceDocument(documentItem);// check if the document has to be skipped or replaced

                    if (!SKIPPED.equals(documentItem.getStatus())) {
                        upload(documentItem);
                        copyAssociations(documentItem);
                        validateMigration(documentItem);
                    }

                    processSuccess(stopwatch, documentItem);

                } catch (Exception e) {
                    processException(documentItem, e);
                }
            }
        }
    }

    /**
     * Migrates document associations
     * @param documentItem
     * @throws Exception
     */
    private void copyAssociations(DocumentItem documentItem) throws Exception {
        if (Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_MIGRATION_COPY_ASSOCIATIONS))) {
            OperationContext operationContext = jobParameters.getSessionSource().createOperationContext(null, false, false, false, IncludeRelationships.SOURCE, null, false, null, false, 50);
            Document document = (Document) jobParameters.getSessionSource().getObject(documentItem.getDocumentId(), operationContext);

            if (document.getRelationships() != null && document.getRelationships().size() > 0) {
                for (Relationship oneRelation : document.getRelationships()) {
                    String associationsSourceId = oneRelation.getSourceId().getId();
                    String associationsTargetId = oneRelation.getTargetId().getId();
                    String type = oneRelation.getType().getId();
                    DocumentAssociation association = new DocumentAssociation(associationsSourceId, associationsTargetId, type);
                    documentItem.getAssociations().add(association);
                    
                }
            }
        }
    }

    /**
     * Sets documents attributes: properties, filename, target destination folder
     * @param documentItem
     * @throws Exception
     */
    private void setDocumentItemAttributes(DocumentItem documentItem) throws Exception {
        String sourceNodeId = NodeRefUtil.removeVersionLabel(documentItem.getDocumentId());
        DocumentProperties documentProperties = alfrescoAPIService.getVersionProperties(null, sourceNodeId);
        JSONObject metadata = documentProperties.getDocumentProperties();
        String fileName = (String) ((JSONObject) metadata.get("properties")).get(PROPERTY_NAME);
        String targetDestinationFolder = getDestinationPath(metadata, documentItem);
        documentItem.setAttributes(sourceNodeId, fileName, targetDestinationFolder, jobParameters, metadata, documentProperties);
    }

    /**
     * Validates migration
     * @param documentItem
     * @throws Exception
     */
    private void validateMigration(DocumentItem documentItem) throws Exception {
        // first validation by path and filename
        Document document = CmisHelper.getDocumentByPath(jobParameters.getSessionTarget(), documentItem.getTargetDestinationFolder(), documentItem.getFilename());
        Exception e;

        if (document == null) { // second validation by nodeId
            document = (Document) jobParameters.getSessionTarget().getObject(documentItem.getTargetNodeRef());

            if (document == null)
                e = new Exception("Document not found in target repository. It has been deleted immediately after being migrated, Target Node ID: " + NodeRefUtil.removeVersionLabel(documentItem.getTargetNodeRef()) + ", Target Destination Path: " + documentItem.getTargetDestinationFolder());

            else
                e = new Exception("Document is not in the expected destination path. Target Node ID: " + NodeRefUtil.removeVersionLabel(documentItem.getTargetNodeRef()) + ", Expected Destination Path: " + documentItem.getTargetNodeRef());

            rollback(documentItem.getTargetNodeRef(), e);
            throw e;

        } else if (!document.getId().equals(NodeRefUtil.removeWorkSpace(documentItem.getTargetNodeRef()))) {
            if (!NodeRefUtil.removeVersionLabel(document.getId()).equals(NodeRefUtil.removeWorkSpace(NodeRefUtil.removeVersionLabel(documentItem.getTargetNodeRef())))) {
                e = new Exception("Document found in the destination path doesn't match the expected nodeId, Expected Node ID: " + NodeRefUtil.removeWorkSpace(documentItem.getTargetNodeRef()) + ", Actual Node ID: " + document.getId() + ", Target Destination Path: " + documentItem.getTargetDestinationFolder());
                rollback(documentItem.getTargetNodeRef(), e);
                throw e;

            } else // this case is not a failure, it's just a warning
                documentItem.setMessage("Document exists but version doesn't match, Expected: " + NodeRefUtil.removeWorkSpace(documentItem.getTargetNodeRef()) + ", Found: " + document.getId());
        }
    }

    /**
     * Gets folder destination path
     * @param documentProperties
     * @param documentItem
     * @return
     * @throws ParseException
     * @throws IllegalAccessException
     */
    private String getDestinationPath(JSONObject documentProperties, DocumentItem documentItem) throws ParseException, IllegalAccessException {
        TimeUtil.resume(FOLDER_DESTINATION, jobParameters.getStopWatchDestinationFolder());
        String rootFolder = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_ROOT_PATH);

        // migrating entire folder structure, document already has a destination path
        if (documentItem.getFolderPath() != null && !documentItem.getFolderPath().isEmpty()) {
            TimeUtil.suspend(FOLDER_DESTINATION, jobParameters.getStopWatchDestinationFolder());
            return rootFolder + documentItem.getFolderPath();

        } else {
            if (Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_KEEP_ORIGINAL))) { //for DELTA documents we need to find out the parent folder
                Document document = (Document) jobParameters.getSessionSource().getObject(documentItem.getDocumentId());
                List<String> pathList = document.getPaths();
                String path = pathList.get(0);
                documentItem.setFolderPath(path.substring(0, path.lastIndexOf("/")));
                return rootFolder + documentItem.getFolderPath();

            } else { // migrating only documents, destination path hast to be determined based on the migration configuration rules
                String targetFolderDestination = documentService.getDestinationPath(documentProperties, jobParameters.getItemList(), jobParameters.getReplaceStringInDestinationPath(), rootFolder);
                TimeUtil.suspend(FOLDER_DESTINATION, jobParameters.getStopWatchDestinationFolder());
                return targetFolderDestination;
            }
        }
    }

    /**
     * Uploads a document into the target repostiory
     * @param documentItem
     * @return
     * @throws Exception
     */
    public DocumentItem upload(DocumentItem documentItem) throws Exception {
        boolean migrateAllVersions = Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_MIGRATION_COPY_ALL_VERSIONS));

        if (migrateAllVersions) {
            return uploadAllVersions(documentItem);

        } else {
            return uploadCurrentVersion(documentItem);
        }
    }

    /**
     * Uploads only latest document version
     * @param documentItem
     * @return
     * @throws Exception
     */
    public DocumentItem uploadCurrentVersion(DocumentItem documentItem) throws Exception {
        File file = alfrescoAPIService.getVersionFile(documentItem.getSourceNodeRef(), null, documentItem.getFilename(), true);
        logger.debug("Temp file created: " + file.getAbsolutePath());
        documentItem.setVersionLabel("1.0");
        documentItem = uploadDocument(file, documentItem);

        if (!documentItem.getStatus().equals(SKIPPED)) {
            alfrescoAPIService.updateNodeProperties(documentItem, documentItem.getDocumentProperties());
            updateNodeCollections(documentItem, documentItem.getDocumentProperties());
        }

        return documentItem;
    }

    /**
     * Uploads all document versions
     * @param documentItem
     * @return
     * @throws Exception
     */
    public DocumentItem uploadAllVersions(DocumentItem documentItem) throws Exception {
        JSONArray versionArray = alfrescoAPIService.getVersionArray(documentItem);

        if (versionArray != null) {
            try {
                boolean currentVersion;
                //older versions first
                for (int i = versionArray.length() - 1; i > -1; i--) {
                    JSONObject currentObject = versionArray.getJSONObject(i);
                    String currentVersionNodeRef = (String) currentObject.get("nodeRef");
                    String nextVersionNodeId = getNextNodeId(versionArray, i);
                    String versionLabel = (String) currentObject.get("label");
                    currentVersion = (i == 0) ? true : false;
                    documentItem.setVersionAttributes(versionLabel, versionArray.length());
                    documentItem = uploadVersionFile(documentItem, currentVersionNodeRef, currentVersion);

                    if (documentItem.getStatus().equals(SKIPPED)) {
                        return documentItem;

                    } else {
                        documentItem = updateProperties(documentItem, currentVersion, nextVersionNodeId);
                        //datasourceService.updateAlfNode(documentItem);
                    }
                }

            } catch (Exception e) {
                rollback(documentItem.getTargetNodeRef(), e);
                throw e;
            }
        }

        return documentItem;
    }

    /**
     * Updates document properties
     * @param documentItem
     * @param currentVersion
     * @param versionNodeId
     * @return
     * @throws Exception
     */
    private DocumentItem updateProperties(DocumentItem documentItem, boolean currentVersion, String versionNodeId) throws Exception {
        DocumentProperties documentProperties;

        if (currentVersion) { // latest version
            documentProperties = documentItem.getDocumentProperties();

        } else { // previous versions
            documentProperties = alfrescoAPIService.getVersionProperties(versionNodeId, documentItem.getSourceNodeRef());
        }

        alfrescoAPIService.updateNodeProperties(documentItem, documentProperties);
        updateNodeCollections(documentItem, documentProperties);

        return documentItem;
    }

    /**
     * Uploads a version document
     * @param documentItem
     * @param currentVersionNodeRef
     * @param currentVersion
     * @return
     * @throws Exception
     */
    private DocumentItem uploadVersionFile(DocumentItem documentItem, String currentVersionNodeRef, boolean currentVersion) throws Exception {
        logger.debug("VERSION: " + documentItem.getVersionLabel());
        File file = alfrescoAPIService.getVersionFile(documentItem.getSourceNodeRef(), NodeRefUtil.removeWorkSpace(currentVersionNodeRef), documentItem.getFilename(), currentVersion);
        logger.debug("Temp file created: " + file.getAbsolutePath());

        documentItem = uploadDocument(file, documentItem);
        logger.debug("File version uploaded. Version node ref: " + currentVersionNodeRef);

        // delete the document once the doc has been uploaded
        if (!file.delete())
            logger.warn("File not deleted form temp folder: " + documentItem.getFilename());

        return documentItem;
    }

    /**
     * Removes a document from the target repository
     * @param targetNodeId
     * @param e
     * @throws Exception
     */
    private void rollback(String targetNodeId, Exception e) throws Exception {
        try {
            targetNodeId = NodeRefUtil.removeVersionLabel(targetNodeId);
            if (targetNodeId != null && !targetNodeId.isEmpty()) {
                if (!targetNodeId.startsWith(WORKSPACE))
                    targetNodeId = WORKSPACE + targetNodeId;

                CmisHelper.deleteDocument(jobParameters.getSessionTarget(), targetNodeId);
                logger.info("Rollback, Document deleted: " + targetNodeId + ", Exception: " + e.getMessage());

            } else {
                logger.error("Rollback cannot be completed, target node ID is null");
            }

        } catch (Exception ex) {
            throw new Exception("Unable to delete document: " + targetNodeId + ", Exception1: " + e.getMessage() + ", Exception2: " + ex.getMessage());
        }
    }

    /**
     * Gets next document id from a JSON array
     * @param versionArray
     * @param i
     * @return null if the array position is the first one
     */
    private String getNextNodeId(JSONArray versionArray, int i) {
        if (i > 0) {
            JSONObject nextObject = versionArray.getJSONObject(i - 1);
            String nextVersionNodeRef = (String) nextObject.get("nodeRef");
            return NodeRefUtil.removeVersionLabel(nextVersionNodeRef);

        } else
            return null;
    }

    /**
     * Verifies if the document can be skipped
     * @param documentItem
     * @return
     */
    private DocumentItem skipReplaceDocument(DocumentItem documentItem) {
        boolean skip = Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_MIGRATION_SKIP_DOCUMENTS));
        boolean replace = Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_MIGRATION_REPLACE_DOCUMENTS));

        if (skip || replace) {
            StopWatch sw = TimeUtil.startStopWatch();

            try {
                Document document = CmisHelper.getDocumentByPath(jobParameters.getSessionTarget(), documentItem.getTargetDestinationFolder(), documentItem.getFilename());
                documentItem.setTargetNodeRef(WORKSPACE + document.getId());

                if (skip) {
                    documentItem.setStatus(SKIPPED);
                    logger.debug("Time: " + sw + ", Skipped document that already exists: " + document.getId());

                } else if (replace) {
                    documentItem.setStatus(REPLACE);
                    CmisHelper.deleteDocument(jobParameters.getSessionTarget(), NodeRefUtil.removeVersionLabel(documentItem.getTargetNodeRef()));
                    documentItem.setTargetNodeRef(null);
                    logger.debug("Time: " + sw + ", Removed document that already exists to replace it: " + document.getId());
                }

                return documentItem;

            } catch (CmisObjectNotFoundException e) {
                logger.debug("Time to check if the document exists: " + sw);
                // document does not exist, do nothing
            }
        }

        return documentItem;
    }

    /**
     * Uploads a document into a repository
     *
     * @return document information
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     * @throws ParseException
     */
    public DocumentItem uploadDocument(File file, DocumentItem documentItem) throws Exception {
        TimeUtil.resume("UPLOAD", jobParameters.getStopWatchUpload());
        logger.debug("Uploading document: " + documentItem.getFilename() + ", destination: " + documentItem.getTargetDestinationFolder());

        JSONObject jsonResponse = alfrescoAPIService.upload(documentItem, file);
        logger.debug("Document uploaded successfully: " + documentItem.getFilename() + ";" + documentItem.getVersionLabel());

        if (!REPLACE.equals(documentItem.getStatus())) // set status SUCCESS except for documents replaced
            documentItem.setStatus(SUCCESS);

        documentItem.setTargetNodeRef(jsonResponse.get("nodeRef") + ";" + documentItem.getVersionLabel());
        TimeUtil.suspend("UPLOAD", jobParameters.getStopWatchUpload());

        return documentItem;
    }

    /**
     * Updates document collection
     * @param documentItem
     * @param documentProperties
     */
    private void updateNodeCollections(DocumentItem documentItem, DocumentProperties documentProperties) {
        Map<String, List<String>> collectionProperties = documentProperties.getCollectionProperties();
        String nodeId = NodeRefUtil.removeVersionLabel(NodeRefUtil.removeWorkSpace(documentItem.getTargetNodeRef()));
        if (collectionProperties != null && collectionProperties.size() > 0) {
            CmisObject document = jobParameters.getSessionTarget().getObject(nodeId);
            document.updateProperties(collectionProperties);
            logger.debug("Collections updated, Node ID: " + nodeId + ", collections: " + collectionProperties.size() + " items");
        } else {
            logger.trace("Node ID: " + nodeId + ", No collection properties");
        }
    }

    /**
     * Process migration document success
     * @param stopWatch
     * @param documentItem
     * @throws Exception
     */
    private void processSuccess(StopWatch stopWatch, DocumentItem documentItem) throws Exception {
        TimeUtil.resume("REPORT", jobParameters.getStopWatchReport());
        documentItem.setSuccessFlag("true");
        TimeUtil.suspend("REPORT", jobParameters.getStopWatchReport());
    }

    /**
     * Process migration document exception
     * @param documentItem
     * @param e
     * @throws Exception
     */
    private void processException(DocumentItem documentItem, Exception e) throws Exception {
        logger.error("EXCEPTION - " + documentItem.getSourceNodeRef() + ", Exception: " + e.getMessage());
        
        documentItem.setStatus(FAILED);
        documentItem.setSuccessFlag("false");

        if (e instanceof NullPointerException || logger.isDebugEnabled()) {
            e.printStackTrace();
        }
    }

 

    /**
     * Gets document entry status for the source repository
     * @param type
     * @return
     */
    private String getSourceStatus(String type) {
        String action = "";

        if (type.equals(SUCCESS)) {
            action = jobParameters.getSuccessAction();

        } else if (type.equals(ERROR)) {
            action = jobParameters.getErrorAction();
        }

        switch (action.toUpperCase()) {
            case MOVE:
                return "MOVED";
            case KEEP:
                return "KEPT IN SAME FOLDER";
            case DELETE:
                return "DELETED";
            default:
                return "KEPT IN SAME FOLDER";
        }
    }
}
