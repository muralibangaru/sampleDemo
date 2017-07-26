package com.ecm.alfresco.migration.job.tasklet;

import com.ecm.alfresco.migration.bean.access.AccessDetails;
import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.bean.folderStructure.*;
import com.ecm.alfresco.migration.component.DataSourceComponent;
import com.ecm.alfresco.migration.job.config.MigrationProperties;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.service.*;
import com.ecm.alfresco.migration.util.CmisHelper;
import com.ecm.alfresco.migration.util.ExcelUtil;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class InitTasklet implements Tasklet {
    @Autowired
    private JobParameters jobParameters;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private LoggerService loggerService;
    @Autowired
    private DataSourceComponent dataSourceComponent;
    private static final Logger logger = Logger.getLogger(InitTasklet.class);
    private String[] propertyFilter;

    /**
     * Executes the initial tasklet
     * @param contribution
     * @param chunkContext
     * @return
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        try {
            initValidation();
            initParameters();
            initFolders();
            validationService.validateRepository(jobParameters.getSessionSource(), jobParameters.getSessionTarget());
            loggerService.initBatchLog4JConfiguration(jobParameters.getBatchFolder());

        } catch (IllegalArgumentException e) {
            sendNotificationError(e);
            throw e;

        } catch (CmisConnectionException e) {
            sendNotificationError(e);
            throw new RuntimeException("It's not possible to establish connection with the repository. " + e.getMessage());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            
            throw e;
        }

        logger.info("INIT FINISHED");
        return RepeatStatus.FINISHED;
    }

    /**
     * Sends a notification error including the exception occurred
     * @param e
     */
    private void sendNotificationError(Exception e) {
        logger.error(e.getMessage());
        
    }

    /**
     * Initializes the excel report
     */
    private void initExcelReport() {
        DocumentItem documentItem = new DocumentItem("Source Node ID", "File Name", "Target Destination Folder", "Target Node ID", null, "Target Node Status", "Message", null, null);
        ExcelUtil.addMigrationRow(jobParameters.getMigrationReport(), "Date Time", "Source Node Status", "Source Destination Folder", documentItem);
        ExcelUtil.addErrorRow(jobParameters.getErrorReport(), "Date Time", "Source Node Status", "Source Destination Folder", documentItem);
    }

    /**
     * Validates migration properties
     */
    private void initValidation() {
        validationService.validateProperties();
    }

    /**
     * Sets job parameters and creates database tables if any
     */
    private void initParameters() {
        jobParameters.setSessionSource(getSession("source"));
        jobParameters.setSessionTarget(getSession("target"));
        jobParameters.setAccessDetailsSource(getAccessDetails("source"));
        jobParameters.setAccessDetailsTarget(getAccessDetails("target"));
        jobParameters.setPageSize(Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_SOURCE_PAGE_SIZE)));
        jobParameters.setQuery(MigrationProperties.get(MigrationProperties.PROP_SOURCE_QUERY));
        jobParameters.setItemList(getFolderStructureItemList());
        jobParameters.setPropertyFilter(getPropertyFilter());
        jobParameters.setReplaceStringInDestinationPath(getReplaceStringArray());
        jobParameters.setNamespacePrefixMap(getNamespacePrefixList());
        jobParameters.setBatchId(getBatchId());
        jobParameters.getStopWatchTotal().start();
        jobParameters.setSuccessAction(MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION));
        jobParameters.setErrorAction(MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION));
        jobParameters.setSuccessFolder(MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_FOLDER) + "/" + jobParameters.getBatchId());
        jobParameters.setErrorFolder(MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_FOLDER) + "/" + jobParameters.getBatchId());
        jobParameters.setCurrentFolder(getRootFolder());
        jobParameters.setSkipDocuments(Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_MIGRATION_COPY_FOLDERS_ONLY)));
        
    }

    /**
     * Gets source root folder
     * @return
     */
    private Folder getRootFolder() {
        if(!MigrationProperties.get(MigrationProperties.PROP_MIGRATION_PROFILE).equalsIgnoreCase("DELTA")) {
            String folderPath = MigrationProperties.get(MigrationProperties.PROP_SOURCE_LOCATION);

            if (folderPath != null && folderPath.contains("/")) {
                Folder rootFolder = CmisHelper.getFolder(jobParameters.getSessionSource(), folderPath);
                return rootFolder;

            } else
                return null;
        } else
            return null;
    }

    /**
     * Create folder structure
     */
    private void initFolders() {
        //createFileSystemSubfolders();
        //createSourceRepositorySubfolders();
    }
/*
    private void createFileSystemSubfolders() {
        Calendar today = new GregorianCalendar();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH) + 1;
        String folderPath = MigrationProperties.get(MigrationProperties.PROP_MIGRATION_REPORT_FOLDER);
        createFolder(folderPath);
        createFolder(folderPath + "/" + year);
        createFolder(folderPath + "/" + year + "/" + month);
        createFolder(folderPath + "/" + year + "/" + month + "/" + jobParameters.getBatchId());
        jobParameters.setBatchFolder(folderPath + "/" + year + "/" + month + "/" + jobParameters.getBatchId());
    }*/

  /*  private void createSourceRepositorySubfolders() {
        CmisHelper.createFolder(jobParameters.getSessionSource(), jobParameters.getSuccessFolder(), null);
        CmisHelper.createFolder(jobParameters.getSessionSource(), jobParameters.getErrorFolder(), null);
    }*/

    /**
     * Creates a folder if it doesn't exist
     *
     * @param path folder path
     * @return created folder
     */
    public File createFolder(String path) {
        if (path != null) {
            if (path.indexOf('/') != path.length() - 1)
                path += "/";

            File folder = new File(path);

            if (!folder.exists())
                folder.mkdir();

            logger.debug("Folder " + path + " CREATED");
            return folder;

        } else {
            logger.debug("Folder " + path + " NOT created");
            return null;
        }
    }

    /**
     * Gets a CMIS session
     *
     * @param type repository type, source or target
     * @return CMIS session from the node number i
     */
    private Session getSession(String type) {
        String hostUrl = MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_HOST_URL);
        String user = MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_USER);
        String password = MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_PASSWORD);
        String cmisUrl = MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_CMIS_URL);
        logger.info("Getting session " + type + ": " + hostUrl + ", user: " + user);

        return CmisHelper.getSession(hostUrl, user, password, cmisUrl);
    }

    private AccessDetails getAccessDetails(String type) {
        String user = MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_USER);
        String password = MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_PASSWORD);
        logger.debug("Getting Access Details " + type + ", user: " + user);
        return new AccessDetails(user, password);
    }

    /**
     * Gets today date in String format
     *
     * @return date string
     */
    public static String getTodayDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        return simpleDateFormat.format(new Date());
    }

    private String getBatchId() {
        Calendar date = new GregorianCalendar();
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        String today = getTodayDate();
        String batchId = today;
        /*//String path = MigrationProperties.get(MigrationProperties.PROP_MIGRATION_REPORT_FOLDER) + "/" + year + "/" + month + "/" + batchId;
        int i = 1;

        while (Files.exists(Paths.get(path))) {
            batchId = today + "_" + i;
            path = MigrationProperties.get(MigrationProperties.PROP_MIGRATION_REPORT_FOLDER) + "/" + year + "/" + month + "/" + batchId;
            i++;
        }*/

        return batchId;
    }


    public List<FolderStructureItem> getFolderStructureItemList() {
        List<FolderStructureItem> itemList = new ArrayList<>();
        String structureItemIDList = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_BASED_ON);
        if (structureItemIDList != null && !structureItemIDList.isEmpty()) {
            String[] itemIDArray = structureItemIDList.split(",");
            for (String oneItemID : itemIDArray) {
                if (oneItemID != null && !oneItemID.isEmpty()) {
                    if (oneItemID.toUpperCase().contains("PROPERTY"))
                            itemList.add(getItemProperty(oneItemID.toLowerCase()));

                }
            }
        }
        return itemList;
    }

    private FolderStructureProperty getItemProperty(String propertyID) {
        String[] propertyList = null;
        String dateFormat = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT + propertyID);
        int levels = Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_LEVELS + propertyID));
        boolean includeLastLevel = Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_INCLUDE_LAST_LEVEL_ENABLED + propertyID));
        String propertyListString = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_LIST + propertyID);

        if (propertyListString != null && !propertyListString.isEmpty()) {
            propertyList = propertyListString.split(",");
        }

        return new FolderStructureProperty(propertyID, propertyList, dateFormat, levels, includeLastLevel);
    }

   /* private static FolderStructureItem getItemRule(String ruleID) {
        
        String ruleString = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_RULE + ruleID);
        if(ruleString != null && !ruleString.isEmpty()) {
            String[] ruleArray = ruleString.split(";");

            for (String oneRuleString : ruleArray) {
                String[] oneRuleArray = oneRuleString.split(",");
                if (oneRuleArray.length == 4)
                    ruleList.add(new Rule(oneRuleArray[0], oneRuleArray[1], oneRuleArray[2], oneRuleArray[3]));
            }

            return new FolderStructureRule(ruleID, ruleList);

        } else {
            throw new IllegalArgumentException(ruleID + " has not been set");
        }
    }*/

    private static FolderStructureItem getItemDate(String dateID) {
        String dateFormat = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT + dateID);
        int levels = Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_LEVELS + dateID));
        return new FolderStructureDate(dateID, dateFormat, levels);
    }

    private List<String[]> getReplaceStringArray() {
        List<String[]> replaceList = new ArrayList<>();
        String replaceStringList = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_REPLACE);
        String stringListArray[] = replaceStringList.split(";");

        for (String oneReplaceString : stringListArray) {
            String oneReplaceArray[] = oneReplaceString.split(",");
            if (oneReplaceArray.length == 2) {
                replaceList.add(oneReplaceArray);
            }
        }
        return replaceList;
    }

    private Map<String, String> getNamespacePrefixList() {
        Map<String, String> namespacePrefixMap = new HashMap<>();
        String stringList = MigrationProperties.get(MigrationProperties.PROP_MIGRATION_NAMESPACE_PREFIX_LIST);
        String stringListArray[] = stringList.split(";");

        for (String onePairValue : stringListArray) {
            String onePairValueArray[] = onePairValue.split(",");
            if (onePairValueArray.length == 2) {
                namespacePrefixMap.put(onePairValueArray[0], onePairValueArray[1]);
            }
        }
        return namespacePrefixMap;
    }

    public String[] getPropertyFilter() {
        String propertyFilter = MigrationProperties.get(MigrationProperties.PROP_TARGET_PROPERTY_FILTER);
        if (propertyFilter != null && !propertyFilter.isEmpty())
            return propertyFilter.split(",");
        else
            return new String[0];
    }
}
