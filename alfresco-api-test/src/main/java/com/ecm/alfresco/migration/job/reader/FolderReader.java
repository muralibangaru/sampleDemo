package com.ecm.alfresco.migration.job.reader;

import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.bean.folder.FolderChildren;
import com.ecm.alfresco.migration.job.config.MigrationProperties;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.service.AlfrescoAPIService;
import com.ecm.alfresco.migration.service.DocumentService;
import com.ecm.alfresco.migration.util.CmisHelper;
import com.ecm.alfresco.migration.util.HttpPoolHelper;
import com.ecm.alfresco.migration.util.TimeUtil;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

public class FolderReader implements ItemReader<DocumentItem> {
    @Autowired
    private JobParameters jobParameters;
    @Autowired
    private AlfrescoAPIService alfrescoAPIService;
   
    private static final Logger logger = Logger.getLogger(FolderReader.class);

    /**
     * Reads a document and passes it to the writer
     * @return
     */
    @Override
    public DocumentItem read() {
        try {
            TimeUtil.resume("NODEREF", jobParameters.getStopWatchGetNodeRef());
            DocumentItem documentItem = getNextFolderDocument();

            if (documentItem != null) {
                logger.debug("Document read: " + documentItem.getDocumentId());
                TimeUtil.suspend("NODEREF", jobParameters.getStopWatchGetNodeRef());
                return documentItem;
            }

        } catch (Exception e) {
            TimeUtil.suspend("NODEREF", jobParameters.getStopWatchGetNodeRef());
            HttpPoolHelper.closeAllConnections();

        }

        return null;
    }

    /**
     * Gets next document from a folder
     * @return
     * @throws Exception
     */
    public DocumentItem getNextFolderDocument() throws Exception {
        DocumentItem oneDocumentItem = jobParameters.getNextFolderDocument();

        if (oneDocumentItem == null) {
            String rootFolder = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_ROOT_PATH);
            FolderChildren pageFolderChildren = getNextPageByFolderPath(rootFolder);

            if (pageFolderChildren == null) {
                logger.debug("No more documents left to process");
                return null;

            } else {
                return getNextFolderDocument();
            }

        } else {
            logger.info(Thread.currentThread().getName() + " - " + jobParameters.getCounter().increaseCounterExtractedDocs() + " - EXTRACTED Document: " + oneDocumentItem.getDocumentId());
            return oneDocumentItem;
        }
    }

    /**
     * Gets next page of children from a folder
     * @param rootFolder
     * @return
     * @throws Exception
     */
    public synchronized FolderChildren getNextPageByFolderPath(String rootFolder) throws Exception {
        if (jobParameters.getCurrentFolder() == null) {
            return null; // there are no more subfolders and/or documents
        }

        FolderChildren folderChildren = CmisHelper.getFolderChildren(jobParameters.getSessionSource(), getClearPath(jobParameters.getCurrentFolder().getPath()), jobParameters.getCounter().increasePageIndex(), jobParameters.getPageSize(), jobParameters.isSkipDocuments());
        logger.info("Extracting Folder: " + jobParameters.getCurrentFolder().getProperty(PropertyIds.NAME).getFirstValue() + ", Page: " + jobParameters.getCounter().getPageIndex() + ", Subfolders: " + folderChildren.getFolderQueue().size() + ", Documents: " + folderChildren.getDocumentQueue().size() + ", Objects: " + folderChildren.getObjectQueue().size());

        if (folderChildren.getDocumentQueue().size() == 0 && folderChildren.getFolderQueue().size() == 0) {
            // there are no more items(docs or subfolders) in the current folder
            createCurrentFolder(rootFolder);
            setNewCurrentFolder();
            return getNextCurrentFolderPage(rootFolder);

        } else {
            addAllChildren(folderChildren);
            return folderChildren;
        }
    }

    /**
     * Creates a folder in the target repository
     * @param rootFolder
     * @throws Exception
     */
    private void createCurrentFolder(String rootFolder) throws Exception {
        String folderPath = rootFolder + getClearPath(jobParameters.getCurrentFolder().getPath());
        Folder newFolder = CmisHelper.createFolder(jobParameters.getSessionTarget(), folderPath, null);
        // copy permissions
        if(Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_MIGRATION_COPY_PERMISSIONS))) {
            alfrescoAPIService.setNodePermissions(jobParameters.getCurrentFolder().getId(), newFolder.getId(), jobParameters, folderPath); // set folder permissions
        }
    }

    /**
     * Gets next current folder page
     * @param rootFolder
     * @return
     * @throws Exception
     */
    private FolderChildren getNextCurrentFolderPage(String rootFolder) throws Exception {
        if (jobParameters.getCurrentFolder() == null) {
            return null; // there are no more subfolders and/or documents

        } else {
            logger.debug("New current folder: " + jobParameters.getCurrentFolder().getPath());
            return getNextPageByFolderPath(rootFolder);
        }
    }

    /**
     * Sets new current folder
     */
    private void setNewCurrentFolder() {
        jobParameters.getCounter().resetPageIndex();
        jobParameters.setCurrentFolder(jobParameters.getNextFolder());

    }

    /**
     * Adds all folders and documents to the job queue
     * @param folderChildren
     */
    private void addAllChildren(FolderChildren folderChildren) {
        jobParameters.addAllFolder(folderChildren.getFolderQueue());
        jobParameters.addAllDocument(folderChildren.getDocumentQueue());
    }

    /**
     * Replaces an unwanted character in the path
     * @param path
     * @return
     */
    private String getClearPath(String path) {
        return path.replace("\\u2019", "’");

    }
}

