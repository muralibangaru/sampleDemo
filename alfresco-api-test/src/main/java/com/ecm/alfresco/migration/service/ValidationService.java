package com.ecm.alfresco.migration.service;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.ecm.alfresco.migration.job.config.MigrationProperties;

import java.text.SimpleDateFormat;

public class ValidationService {
    private static final String NOT_ALLOWED_CHARACTERS_FILENAME = "[\\/:\\\\\"*?<>|]+";
    private static final String NOT_ALLOWED_CHARACTERS_PATH = "[:.\"*?<>|]+";
    private static final String MOVE = "MOVE";
    private static final String KEEP = "KEEP";
    private static final String DELETE = "DELETE";
    private static final String PROPERTY = "PROPERTY";
    private static final String DATE = "DATE";
    private static final String RULE = "RULE";
    private static final Logger logger = Logger.getLogger(ValidationService.class);

    /**
     * Validates properties
     *
     * @throws IllegalArgumentException
     */
    public void validateProperties() throws IllegalArgumentException {
        
        validateParam("source." + MigrationProperties.PROP_ALFRESCO_USER);
        validateParam("target." + MigrationProperties.PROP_ALFRESCO_USER);
        validateParam("source." + MigrationProperties.PROP_ALFRESCO_PASSWORD);
        validateParam("target." + MigrationProperties.PROP_ALFRESCO_PASSWORD);
        validateParam("source." + MigrationProperties.PROP_ALFRESCO_HOST_URL);
        validateParam("target." + MigrationProperties.PROP_ALFRESCO_HOST_URL);
        validateParam(MigrationProperties.PROP_SOURCE_BATCH_SIZE);
        validateParam(MigrationProperties.PROP_SOURCE_PAGE_SIZE);
        validateParam(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION);
        validateParam(MigrationProperties.PROP_SOURCE_ERROR_ACTION);

        if (MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION) != null &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION).isEmpty() &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION).equals(KEEP) &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION).equals(MOVE) &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION).equals(DELETE)){
            throw new IllegalArgumentException("Property " + MigrationProperties.PROP_SOURCE_ERROR_ACTION + " is invalid. It must be either 'KEEP' or 'MOVE' or 'DELETE'");
        }

        if (MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION) != null &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION).isEmpty() &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION).equals(KEEP) &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION).equals(MOVE) &&
                !MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION).equals(DELETE)){
            throw new IllegalArgumentException("Property " + MigrationProperties.PROP_SOURCE_SUCCESS_ACTION + " is invalid. It must be either 'KEEP' or 'MOVE' or 'DELETE'");
        }

        if (MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION).equals(DELETE))
            logger.warn("Source documents will be deleted after migration. Property: " + MigrationProperties.PROP_SOURCE_ERROR_ACTION);

        if (MigrationProperties.get(MigrationProperties.PROP_SOURCE_SUCCESS_ACTION).equals(MOVE))
            validateParam(MigrationProperties.PROP_SOURCE_SUCCESS_FOLDER);

        if (MigrationProperties.get(MigrationProperties.PROP_SOURCE_ERROR_ACTION).equals(MOVE))
            validateParam(MigrationProperties.PROP_SOURCE_ERROR_FOLDER);

        if(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_LEVELS) != null &&
                !MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_LEVELS).isEmpty() &&
                !StringUtils.isNumeric(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_LEVELS))) {
            throw new IllegalArgumentException("Property " + MigrationProperties.PROP_TARGET_STRUCTURE_LEVELS + " must be a number");
        }

        if((MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_BASED_ON) != null && MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_BASED_ON).equals(DATE)) ||
                (MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT) != null && !MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT).isEmpty())) {
            validateParam(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT);
            validateDateFormat();
        }

        if (MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_ROOT_PATH).endsWith("/")) {
            throw new IllegalArgumentException("Property " + MigrationProperties.PROP_TARGET_STRUCTURE_ROOT_PATH + " must NOT end with '/'");
        }

        if (MigrationProperties.get(MigrationProperties.PROP_MIGRATION_PROFILE).equalsIgnoreCase("DELTA") &&
                (MigrationProperties.get(MigrationProperties.PROP_SOURCE_QUERY) == null || MigrationProperties.get(MigrationProperties.PROP_SOURCE_QUERY).isEmpty())) {

            throw new IllegalArgumentException("Property " + MigrationProperties.PROP_SOURCE_QUERY + " can't be empty when running DELTA profile");
        }
    }

    /**
     * Validates parameter
     *
     * @param property parameter value
     * @throws IllegalArgumentException
     */
    public void validateParam(String property) throws IllegalArgumentException {
        validateParam(property, null);
    }

    /**
     * Validates parameter
     *
     * @param property parameter value
     * @param message  message error
     * @throws IllegalArgumentException
     */
    public void validateParam(String property, String message) throws IllegalArgumentException {
        String propertyValue = MigrationProperties.get(property);
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            if (message == null)
                message = "Property '" + property + "' is null";

            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates repository connectivity
     */
    public void validateRepository(Session sessionSource, Session sessionTarget) {
        validateConnection(sessionSource, "source");
        validateConnection(sessionTarget, "target");
    }


    /**
     * Validates session connection
     *
     * @param session CMIS session
     * @param type    repository type, source or target
     */
    private static void validateConnection(Session session, String type) {
        RepositoryInfo repoInfo = session.getRepositoryInfo();
        if (repoInfo == null || repoInfo.getId() == null)
            throw new RuntimeException("It's not possible to establish connection with " + MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_HOST_URL));
    }

    /**
     * Validates a file name
     *
     * @param fileName file name
     * @return a valid file name
     * @throws IllegalArgumentException
     */
    public String validateFileName(String fileName) throws IllegalArgumentException {
        if (fileName != null && !fileName.isEmpty()) {
            return fileName;

        } else
            throw new IllegalArgumentException("File Name is null");

    }

    /**
     * Validates folder path
     *
     * @param destinationPath destination folder path
     * @return valid destination path
     */
    public String validateFolderPath(String destinationPath) {
        String path = destinationPath;

        if (destinationPath.endsWith("/")) // remove slash at the end
            path = path.substring(0, destinationPath.length() - 1);

        path = getClearPath(path);

        logger.debug("DestinationPath, before validation: " + destinationPath + ", after validation: " + path);
        return path;
    }

    private String getClearPath(String path) {
        return path;//.replace("\\u2019", "’");
    }

    /**
     * Validate date format
     */
    public void validateDateFormat() {
        try {
            new SimpleDateFormat(MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT));

        } catch (Exception e) {
            throw new IllegalArgumentException("Date format '" + MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT) + "' is not valid");
        }
    }
}
