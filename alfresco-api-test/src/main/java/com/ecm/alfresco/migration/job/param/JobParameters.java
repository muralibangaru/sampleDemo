package com.ecm.alfresco.migration.job.param;

import com.ecm.alfresco.migration.bean.access.AccessDetails;
import com.ecm.alfresco.migration.bean.counter.Counter;
import com.ecm.alfresco.migration.bean.document.DocumentAssociation;
import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.bean.folderStructure.FolderStructureItem;
import com.ecm.alfresco.migration.job.config.MigrationProperties;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class JobParameters {
    private Queue<String> documentQueue = new LinkedList<>();
    private Queue<DocumentItem> folderDocumentQueue = new LinkedList<>();
    private Queue<Folder> folderQueue = new LinkedList<>();
    private Queue<DocumentAssociation> associationQueue = new LinkedList<>();
    private Folder currentFolder;
    private Counter counter = new Counter();
    private StopWatch stopWatchTotal = new StopWatch();
    private StopWatch stopWatchGetNodeRef = new StopWatch();
    private StopWatch stopWatchGetProperties = new StopWatch();
    private StopWatch stopWatchGetFile = new StopWatch();
    private StopWatch stopWatchUpload = new StopWatch();
    private StopWatch stopWatchReport = new StopWatch();
    private StopWatch stopWatchCMISObject = new StopWatch();
    private StopWatch stopWatchDestinationFolder = new StopWatch();
    private Session sessionSource;
    private Session sessionTarget;
    private Workbook migrationReport;
    private Workbook errorReport;
    private int pageSize;
    private String query;    
    private List<FolderStructureItem> itemList;
    private List<String[]> replaceStringInDestinationPath;
    private Map<String, String> namespacePrefixMap;
    private String[] propertyFilter;
    private String batchId;
    private String batchFolder;
    private long numDocumentsToMigrate;
    private String successAction;
    private String successFolder;
    private String errorAction;
    private String errorFolder;
    private AccessDetails accessDetailsSource;
    private AccessDetails accessDetailsTarget;
    private boolean skipDocuments;
    private String reportDBTable;
    private String assocDBTable;

    private static final Logger logger = Logger.getLogger(JobParameters.class);

    /**
     * Gets next document from the folder document queue
     * @return
     */
    public synchronized DocumentItem getNextFolderDocument() {
        logger.trace("Documents in queue:");
        for(DocumentItem oneDocumentItem : folderDocumentQueue) {
            logger.trace(oneDocumentItem.getDocumentId());
        }
        return folderDocumentQueue.poll();
    }

    /**
     * Gets next folder from the folder queue
     * @return
     */
    public synchronized Folder getNextFolder() {
        logger.trace("Folders in queue:");
        for(Folder oneFolder : folderQueue) {
            logger.trace(oneFolder.getName());
        }

        Folder nextFolder = folderQueue.poll();
        if(nextFolder != null) {
            String folderName = (String) nextFolder.getProperty(PropertyIds.NAME).getFirstValue();

            if (folderName.equalsIgnoreCase("WebConversion")) {
                logger.info("Folder Skipped: " + nextFolder.getPath());
                return getNextFolder();

            } else {
                logger.debug("Next folder: " + folderName);
                return nextFolder;
            }
        }

        logger.debug("Next folder: null");

        return null;

    }

    /**
     * Adds documents Id to the document queue. Filters those that belong to a webconversion folder
     * @param itemList
     */
    public synchronized void addAll(ItemIterable<QueryResult> itemList) {
        QueryResult oneQueryResult;
        String documentId;

        while (itemList.iterator().hasNext()) {
            oneQueryResult = itemList.iterator().next();
            documentId = (String) oneQueryResult.getPropertyByQueryName(PropertyIds.OBJECT_ID).getFirstValue();

            if (MigrationProperties.get(MigrationProperties.PROP_MIGRATION_PROFILE).equalsIgnoreCase("DELTA") ||
                    MigrationProperties.get(MigrationProperties.PROP_MIGRATION_PROFILE).equalsIgnoreCase("QUERY")) {
                // verify no document belongs to webconversion folder when running DELTA or QUERY profiles

                Document document = (Document) getSessionSource().getObject(documentId);
                List<String> pathList = document.getPaths();
                String path = pathList.get(0).substring(0, pathList.get(0).lastIndexOf("/"));

                if (path.toLowerCase().contains("webconversion")) {
                    logger.debug(documentId + ", document Skipped because it belongs to WebConversion folder");

                } else {
                    documentQueue.add(documentId);
                }
            }
        }
    }

    /**
     * Adds documents Id to the document queue
     * @param itemList
     */
    public synchronized void addAll(List<String> itemList) {
        for(String oneString : itemList) {
            documentQueue.add(oneString);
        }
    }

    /**
     * Adds documents Id to the folder document queue
     * @param documentList
     */
    public synchronized void addAllDocument(Queue<DocumentItem> documentList) {
        logger.debug("Adding documents into the queue: " + documentList.size());

        for(DocumentItem oneDocumentItem : documentList) {
            folderDocumentQueue.add(oneDocumentItem);
        }
    }

    /**
     * Adds folders to the folder queue
     * @param folderList
     */
    public synchronized void addAllFolder(Queue<Folder> folderList) {
        logger.debug("Adding folders into the queue: " + folderList.size());

        for (Folder oneFolder : folderList) {
            folderQueue.add(oneFolder);
        }
    }

    /**
     * Adds associations to the association queue
     * @param assocList
     */
    public synchronized void addAllAssociation(List<DocumentAssociation> assocList) {
        logger.debug("Adding associations into the queue: " + assocList.size());

        for(DocumentAssociation oneAssociation : assocList) {
            associationQueue.add(oneAssociation);
        }
    }

    /**
     *
     * @return
     */
    public boolean isSkipDocuments() {
        return skipDocuments;
    }

    /**
     *
     * @param skipDocuments
     */
    public void setSkipDocuments(boolean skipDocuments) {
        this.skipDocuments = skipDocuments;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchCMISObject() {
        return stopWatchCMISObject;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchDestinationFolder() {
        return stopWatchDestinationFolder;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchGetNodeRef() {
        return stopWatchGetNodeRef;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchGetProperties() {
        return stopWatchGetProperties;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchGetFile() {
        return stopWatchGetFile;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchUpload() {
        return stopWatchUpload;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchReport() {
        return stopWatchReport;
    }

    /**
     *
     * @return
     */
    public String[] getPropertyFilter() {
        return propertyFilter;
    }

    /**
     *
     * @param propertyFilter
     */
    public void setPropertyFilter(String[] propertyFilter) {
        this.propertyFilter = propertyFilter;
    }

    /**
     *
     * @return
     */
    public AccessDetails getAccessDetailsSource() {
        return accessDetailsSource;
    }

    /**
     *
     * @param accessDetailsSource
     */
    public void setAccessDetailsSource(AccessDetails accessDetailsSource) {
        this.accessDetailsSource = accessDetailsSource;
    }

    /**
     *
     * @return
     */
    public AccessDetails getAccessDetailsTarget() {
        return accessDetailsTarget;
    }

    /**
     *
     * @param accessDetailsTarget
     */
    public void setAccessDetailsTarget(AccessDetails accessDetailsTarget) {
        this.accessDetailsTarget = accessDetailsTarget;
    }

    /**
     *
     * @return
     */
    public List<String[]> getReplaceStringInDestinationPath() {
        return replaceStringInDestinationPath;
    }

    /**
     *
     * @param replaceStringInDestinationPath
     */
    public void setReplaceStringInDestinationPath(List<String[]> replaceStringInDestinationPath) {
        this.replaceStringInDestinationPath = replaceStringInDestinationPath;
    }

    /**
     *
     * @return
     */
    public List<FolderStructureItem> getItemList() {
        return itemList;
    }

    /**
     *
     * @param itemList
     */
    public void setItemList(List<FolderStructureItem> itemList) {
        this.itemList = itemList;
    }

    /**
     *
     * @return
     */
    public Map<String, String> getNamespacePrefixMap() {
        return namespacePrefixMap;
    }

    /**
     *
     * @param namespacePrefixMap
     */
    public void setNamespacePrefixMap(Map<String, String> namespacePrefixMap) {
        this.namespacePrefixMap = namespacePrefixMap;
    }

    /**
     *
     * @param sessionSource
     */
    public void setSessionSource(Session sessionSource) {
        this.sessionSource = sessionSource;
    }

    /**
     *
     * @param sessionTarget
     */
    public void setSessionTarget(Session sessionTarget) {
        this.sessionTarget = sessionTarget;
    }

    /**
     *
     * @param migrationReport
     */
    public void setMigrationReport(Workbook migrationReport) {
        this.migrationReport = migrationReport;
    }

    /**
     *
     * @param errorReport
     */
    public void setErrorReport(Workbook errorReport) {
        this.errorReport = errorReport;
    }

    /**
     *
     * @param pageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     *
     * @param query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     *
     * @param batchId
     */
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    /**
     *
     * @param batchFolder
     */
    public void setBatchFolder(String batchFolder) {
        this.batchFolder = batchFolder;
    }

    /**
     *
     * @return
     */
    public Counter getCounter() {
        return counter;
    }

    /**
     *
     * @return
     */
    public StopWatch getStopWatchTotal() {
        return stopWatchTotal;
    }

    /**
     *
     * @return
     */
    public Session getSessionSource() {
        return sessionSource;
    }

    /**
     *
     * @return
     */
    public Session getSessionTarget() {
        return sessionTarget;
    }

    /**
     *
     * @return
     */
    public Workbook getMigrationReport() {
        return migrationReport;
    }

    /**
     *
     * @return
     */
    public Workbook getErrorReport() {
        return errorReport;
    }

    /**
     *
     * @return
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     *
     * @return
     */
    public String getQuery() {
        return query;
    }

    /**
     *
     * @return
     */
    public String getBatchId() {
        return batchId;
    }

    /**
     *
     * @return
     */
    public String getBatchFolder() {
        return batchFolder;
    }

    /**
     *
     * @return
     */
    public long getNumDocumentsToMigrate() {
        return numDocumentsToMigrate;
    }

    /**
     *
     * @param numDocumentsToMigrate
     */
    public void setNumDocumentsToMigrate(long numDocumentsToMigrate) {
        this.numDocumentsToMigrate = numDocumentsToMigrate;
    }

    /**
     *
     * @return
     */
    public String getSuccessAction() {
        return successAction;
    }

    /**
     *
     * @param successAction
     */
    public void setSuccessAction(String successAction) {
        this.successAction = successAction;
    }

    /**
     *
     * @return
     */
    public String getSuccessFolder() {
        return successFolder;
    }

    /**
     *
     * @param successFolder
     */
    public void setSuccessFolder(String successFolder) {
        this.successFolder = successFolder;
    }

    /**
     *
     * @return
     */
    public String getErrorAction() {
        return errorAction;
    }

    /**
     *
     * @param errorAction
     */
    public void setErrorAction(String errorAction) {
        this.errorAction = errorAction;
    }

    /**
     *
     * @return
     */
    public String getErrorFolder() {
        return errorFolder;
    }

    /**
     *
     * @param errorFolder
     */
    public void setErrorFolder(String errorFolder) {
        this.errorFolder = errorFolder;
    }

    /**
     *
     * @return
     */
    public Folder getCurrentFolder() {
        return currentFolder;
    }

    /**
     *
     * @param currentFolder
     */
    public void setCurrentFolder(Folder currentFolder) {
        this.currentFolder = currentFolder;
    }

    /**
     *
     * @return
     */
    public synchronized String getNextDocumentId() {
        return documentQueue.poll();
    }

    /**
     *
     * @return
     */
    public synchronized DocumentAssociation getNextAssociation() {
        return associationQueue.poll();
    }

    /**
     *
     * @return
     */
    public String getReportDBTable() {
        return reportDBTable;
    }

    /**
     *
     * @param reportDBTable
     */
    public void setReportDBTable(String reportDBTable) {
        this.reportDBTable = reportDBTable;
    }

    /**
     *
     * @return
     */
    public String getAssocDBTable() {
        return assocDBTable;
    }

    /**
     *
     * @param assocDBTable
     */
    public void setAssocDBTable(String assocDBTable) {
        this.assocDBTable = assocDBTable;
    }
}
