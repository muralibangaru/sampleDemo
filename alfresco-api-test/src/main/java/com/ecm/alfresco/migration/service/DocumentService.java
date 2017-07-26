package com.ecm.alfresco.migration.service;

import com.ecm.alfresco.migration.bean.folderStructure.*;
import com.ecm.alfresco.migration.job.config.MigrationProperties;
import com.ecm.alfresco.migration.util.CmisHelper;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DocumentService {
    @Autowired
    private ValidationService validationService;
    @Autowired
   // private DatasourceService datasourceService;
    private static final Logger logger = Logger.getLogger(DocumentService.class);

    private static final String KEEP = "keep";
    private static final String MOVE = "move";
    private static final String DELETE = "delete";
    private static final String SOURCE = "source";
    private static final String TARGET = "target";

    /**
     * Gets destination path for a specific document property
     *
     * @param documentProperties document properties
     * @param itemList
     * @param replaceList
     * @return document destination folder path
     * @throws IllegalArgumentException
     */

    public String getDestinationPath(JSONObject documentProperties, List<FolderStructureItem> itemList, List<String[]> replaceList, String rootFolder) throws IllegalArgumentException, ParseException, IllegalAccessException {
        if(!itemList.isEmpty()) {
            String destinationPath = getDestinationPathFromItemList(documentProperties, itemList, rootFolder);
            return replaceString(destinationPath, replaceList);

        } else
            return rootFolder;
    }

    /**
     * Gets destination folder path
     * @param documentProperties
     * @param itemList
     * @param rootFolder
     * @return
     * @throws IllegalArgumentException
     * @throws ParseException
     * @throws IllegalAccessException
     */
    public String getDestinationPathFromItemList(JSONObject documentProperties, List<FolderStructureItem> itemList, String rootFolder) throws IllegalArgumentException, ParseException, IllegalAccessException {
        String destinationPath = "";

        for(FolderStructureItem oneItem : itemList) {
            if(oneItem instanceof FolderStructureProperty) {
                destinationPath += getPropertyListDestinationPath(documentProperties, (FolderStructureProperty) oneItem);
                if(destinationPath.isEmpty()) {
                    return getDefaultDestination(rootFolder);
                }

            } /*else if(oneItem instanceof FolderStructureRule){
                destinationPath += getRuleDestinationPath(documentProperties, (FolderStructureRule) oneItem);
                if(destinationPath.isEmpty()) {
                    return getDefaultDestination(rootFolder);
                }

            } else if(oneItem instanceof FolderStructureDate) {
                destinationPath += getDateDestinationPath((FolderStructureDate) oneItem);
                if(destinationPath.isEmpty()) {
                    return getDefaultDestination(rootFolder);
                }
            }*/
        }

        return rootFolder + destinationPath;

    }

    /**
     * Gets the default destination folder
     * @param rootFolder
     * @return
     */
    private String getDefaultDestination(String rootFolder) {
        // if no destination found, the root folder + default folder will be the destination path
        String defaultDestination = MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_DEFAULT);
        if (defaultDestination != null && !defaultDestination.isEmpty()) {
            return rootFolder + defaultDestination;
        } else {
            throw new IllegalArgumentException("It's not possible to determine the destination path for the document. If there's no default path configured the document migration will fail");
        }
    }

    /**
     * Gets destination folder based on a date
     * @param itemDate
     * @return
     * @throws ParseException
     * @throws IllegalAccessException
     */
    private String getDateDestinationPath(FolderStructureDate itemDate) throws ParseException, IllegalAccessException {
        Calendar today = new GregorianCalendar();
        String destinationFolder = getDateLevels(today, itemDate.getLevels());

        return destinationFolder;
    }

    /**
     * Gets path based on a date and the number of levels required
     * @param date
     * @param folderLevels
     * @return
     */
    private String getDateLevels(Calendar date, int folderLevels) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);
        int hour = date.get(Calendar.HOUR_OF_DAY);
        int minute = date.get(Calendar.MINUTE);
        int second = date.get(Calendar.SECOND);
        int[] dateLevels = {year, month, day, hour, minute, second};
        String destinationFolder = "";

        if (folderLevels > 0) {
            for (int i = 0; i < folderLevels; i++) {
                destinationFolder += "/" + dateLevels[i];
            }
        }

        return destinationFolder;
    }

    /**
     * Gets destination folder path based on rules
     * @param documentProperties
     * @param itemRule
     * @return
     * @throws IllegalAccessException
     *//*
    private String getRuleDestinationPath(JSONObject documentProperties, FolderStructureRule itemRule) throws IllegalAccessException {
        for (Rule oneRule : itemRule.getRuleList()) {
            String ruleDestinationFolder = oneRule.getDestinationFolder(documentProperties);
            if (ruleDestinationFolder != null) {
                return ruleDestinationFolder;
            }
        }

        return "";
    }*/

    /**
     * Gets destination folder path based on properties
     * @param documentProperties
     * @param itemProperty
     * @return
     * @throws ParseException
     * @throws IllegalAccessException
     */
    public String getPropertyListDestinationPath(JSONObject documentProperties, FolderStructureProperty itemProperty) throws ParseException, IllegalAccessException {
        // check one by one all the property values until one of them is not null to use it as destination path
        for (String onePropertyId : itemProperty.getPropertyList()) {
            String destinationFolder = getPropertyDestinationFolder(documentProperties, itemProperty.getLevels(), itemProperty.isIncludeLastLevelEnabled(), itemProperty.getDateFormat(), onePropertyId);
            if (destinationFolder != null) {
                return destinationFolder;
            }
        }
        return "";
    }

    /**
     * Gets destination folder path based on a specific property
     * @param documentProperties
     * @param folderLevels
     * @param includeLastLevel
     * @param dateFormat
     * @param propertyId
     * @return
     * @throws ParseException
     */
    private String getPropertyDestinationFolder(JSONObject documentProperties, int folderLevels, boolean includeLastLevel, String dateFormat, String propertyId) throws ParseException {
        String destinationFolder = "";

        if (folderLevels > 0 || includeLastLevel) {
            Object propertyValue = getPropertyDestinationValue(documentProperties, propertyId);

            if (propertyValue == null)
                return null;

            String validPropertyValue = getValidPropertyValueFolderLevel(propertyValue, false, dateFormat);

            if (propertyValue instanceof GregorianCalendar) {
                GregorianCalendar propertyDate = (GregorianCalendar) propertyValue;
                destinationFolder = getDateLevels(propertyDate, folderLevels);

            } else {
                for (int i = 0; i < folderLevels; i++) {
                    destinationFolder += "/" + validPropertyValue.substring(0, i + 1);
                }
            }

            if (includeLastLevel) {
                destinationFolder += "/" + getValidPropertyValueFolderLevel(propertyValue, true, dateFormat);
            }

            logger.debug("Value trimmed: " + validPropertyValue + ", Destination: " + destinationFolder);
        }


        return destinationFolder;
    }

    /**
     * Gets a property value
     * @param documentProperties
     * @param propertyId
     * @return
     */
    private Object getPropertyDestinationValue(JSONObject documentProperties, String propertyId) {
        String propertyValue = (String) documentProperties.get(propertyId);

        if (propertyValue == null)
            return null;
        else
            return propertyValue;
    }

    /**
     * Gets a clean property value
     * @param propertyValue
     * @param lastLevel
     * @param dateFormat
     * @return
     * @throws ParseException
     */
    private String getValidPropertyValueFolderLevel(Object propertyValue, boolean lastLevel, String dateFormat) throws ParseException {
        String stringValue = "";

        if (dateFormat != null && !dateFormat.isEmpty()) {
            stringValue = getStringDate((String) propertyValue, dateFormat);

        } else {
            if (propertyValue.toString().startsWith("/"))
                stringValue = propertyValue.toString().substring(1);
            else
                stringValue = propertyValue + "";
        }

        if (lastLevel)
            return stringValue.trim().replaceAll("[:.\"*?<>|]+", "_");
        else
            return stringValue.trim().replace(" ", "_").replaceAll("[:.\"*?<>|]+", "_");
    }

    /**
     * Replaces a list of strings from a value and return the result
     * @param string
     * @param replaceList
     * @return
     */
    private String replaceString(String string, List<String[]> replaceList){
        for(String[] oneReplaceArray : replaceList) {
            String oldSubString = oneReplaceArray[0];
            String newSubString = oneReplaceArray[1];

            if(oldSubString != null && !oldSubString.isEmpty() && newSubString != null && !newSubString.isEmpty())
                string = string.replace(oldSubString, newSubString);
        }

        return string;
    }

    /**
     * Applies a post migration action
     * @param session
     * @param objectId
     * @param action
     * @param destinationFolder
     */
    public void applyAction(Session session, String objectId, String action, String destinationFolder) {
        if (action != null && !action.isEmpty()) {
            switch (action.toLowerCase()) {
                case (KEEP):
                    // do nothing
                    break;

                case (MOVE):
                    CmisHelper.moveDocument(session, objectId, destinationFolder);
                    logger.info("Document MOVED: " + objectId + ", Destination Folder: " + destinationFolder);
                    break;

                case (DELETE):
                    CmisHelper.deleteDocument(session, objectId);
                    logger.info("Document DELETED: " + objectId);
                    break;

                default:
                    // do nothing
                    break;
            }
        }
    }

    /**
     * Gets a string date in a specific format
     * @param date
     * @param dateFormat
     * @return
     * @throws ParseException
     */
    private String getStringDate(String date, String dateFormat) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date inputDate = sdf.parse(date);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        return simpleDateFormat.format(inputDate);
    }
}
