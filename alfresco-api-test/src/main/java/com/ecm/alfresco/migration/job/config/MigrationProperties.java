package com.ecm.alfresco.migration.job.config;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class MigrationProperties {
    
    public static final String PROP_MIGRATION_COPY_ALL_VERSIONS = "migration.copy.all.versions.enabled";
    public static final String PROP_MIGRATION_COPY_PERMISSIONS = "migration.copy.permissions.enabled";
    public static final String PROP_MIGRATION_COPY_FOLDERS_ONLY = "migration.copy.folders.only.enabled";
    public static final String PROP_MIGRATION_COPY_ASSOCIATIONS = "migration.copy.associations.enabled";
    public static final String PROP_MIGRATION_SKIP_DOCUMENTS = "migration.skip.existing.documents.enabled";
    public static final String PROP_MIGRATION_REPLACE_DOCUMENTS = "migration.replace.existing.documents.enabled";
    public static final String PROP_MIGRATION_NAMESPACE_PREFIX_LIST = "migration.namespace.prefix.list";
    public static final String PROP_MIGRATION_DECRYPTION = "migration.decryption.enabled";
    public static final String PROP_MIGRATION_DECRYPTION_HOST = "migration.decryption.host.url";

    public static final String PROP_MIGRATION_PROFILE = "migration.profile";
    public static final String PROP_SOURCE_LOCATION = "source.location";
    public static final String PROP_SOURCE_SUCCESS_ACTION = "source.post-migration.action.success";
    public static final String PROP_SOURCE_SUCCESS_FOLDER = "source.post-migration.folder.success";
    public static final String PROP_SOURCE_ERROR_ACTION = "source.post-migration.action.error";
    public static final String PROP_SOURCE_ERROR_FOLDER = "source.post-migration.folder.error";
    public static final String PROP_TARGET_STRUCTURE_KEEP_ORIGINAL= "target.folder.structure.keep.original.enabled";
    public static final String PROP_TARGET_STRUCTURE_ROOT_PATH = "target.folder.structure.root.path";
    public static final String PROP_TARGET_STRUCTURE_BASED_ON = "target.folder.structure.basedOn";
    public static final String PROP_TARGET_STRUCTURE_LEVELS = "target.folder.structure.levels.";
    public static final String PROP_TARGET_STRUCTURE_PROPERTY_LIST = "target.folder.structure.list.";
    public static final String PROP_TARGET_STRUCTURE_PROPERTY_DATE_FORMAT = "target.folder.structure.date.format.";
    public static final String PROP_TARGET_STRUCTURE_PROPERTY_INCLUDE_LAST_LEVEL_ENABLED = "target.folder.structure.includePropertyValueAsLastLevelFolder.enabled.";
    public static final String PROP_TARGET_STRUCTURE_RULE = "target.folder.structure.";
    public static final String PROP_TARGET_STRUCTURE_DEFAULT = "target.folder.structure.default";
    public static final String PROP_TARGET_STRUCTURE_REPLACE = "target.folder.structure.string.replace";
    public static final String PROP_TARGET_KEEP_SOURCE_NODEID_ENABLED = "target.keep.source.nodeId.enabled";
    public static final String PROP_TARGET_KEEP_SOURCE_NODEID_PROPERTY = "target.keep.source.nodeId.property";
    public static final String PROP_TARGET_NEW_ASPECT_LIST = "target.new.aspect.list";
    public static final String PROP_TARGET_PROPERTY_FILTER = "target.property.filter";
    public static final String PROP_TARGET_NEW_CONTENT_TYPE = "target.new.contentType";
    public static final String PROP_SOURCE_QUERY = "source.query";
    public static final String PROP_SOURCE_PAGE_SIZE = "source.page.size";
    public static final String PROP_SOURCE_BATCH_SIZE = "source.batch.size";
    public static final String PROP_BATCH_LOG_LEVEL = "migration.process.log4j.batch.level";
    public static final String PROP_ALFRESCO_HOST_URL = "alfresco.host.url";
    public static final String PROP_ALFRESCO_CMIS_URL = "alfresco.cmis.url";
    public static final String PROP_ALFRESCO_USER = "alfresco.user";
    public static final String PROP_ALFRESCO_PASSWORD = "alfresco.password";
    public static final String PROP_NUM_THREADS = "migration.process.threads";
    

    private static Map<String, String> properties = new HashMap<String, String>();
    private static List<String> aspectList;
    private static final Logger logger = Logger.getLogger(MigrationProperties.class);

    public static String get(String key) {
        return properties.get(key);
    }

    /**
     * Loads properties from migration.properties
     *
     * @param profile
     */
    public static void loadProperties(String profile) throws Exception {
        setProperties();
        setProfile(profile);
        setAspectList();
    }

    private static void setProperties() throws IOException {
        logger.debug("Loading configuration migration properties");
        Properties propertyFile = new Properties();
        propertyFile.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("migration.properties"));

        if(propertyFile.size() == 0) {
            logger.error("migration.properties file not found");

        } else {
            logger.debug("migration.properties size: " + propertyFile.size());
        }
        for (String oneKey : propertyFile.stringPropertyNames()) {
            properties.put(oneKey, propertyFile.getProperty(oneKey));
            logger.debug("key: '" + oneKey + "', value: '" + propertyFile.getProperty(oneKey) + "'");
        }
    }

    private static void setProfile(String profile) {
        properties.put(PROP_MIGRATION_PROFILE, profile);

        /*if(profile.equalsIgnoreCase("FOLDER")) { // migrate folder structure only
            properties.put(PROP_MIGRATION_COPY_FOLDERS_ONLY, "true");
            properties.put(PROP_MIGRATION_COPY_PERMISSIONS, "true");
            properties.put(PROP_TARGET_STRUCTURE_KEEP_ORIGINAL, "true");

        } else if(profile.equalsIgnoreCase("FAILED")) {
            properties.put(PROP_MIGRATION_REPLACE_DOCUMENTS, "true");
            properties.put(PROP_TARGET_STRUCTURE_KEEP_ORIGINAL, "true");

        } else if(profile.equalsIgnoreCase("DELTA")) { //migrate delta, this requires to set a query in the properties to get only recently modified documents
            properties.put(PROP_MIGRATION_SKIP_DOCUMENTS, "false");
            properties.put(PROP_MIGRATION_REPLACE_DOCUMENTS, "true");
            properties.put(PROP_TARGET_STRUCTURE_KEEP_ORIGINAL, "true");

        } else if(profile.equalsIgnoreCase("RUN")) { // run the migration for the first time
            properties.put(PROP_MIGRATION_SKIP_DOCUMENTS, "false");
            properties.put(PROP_MIGRATION_REPLACE_DOCUMENTS, "false");
            properties.put(PROP_MIGRATION_COPY_PERMISSIONS, "true");
            properties.put(PROP_TARGET_STRUCTURE_KEEP_ORIGINAL, "true");

        } else if(profile.equalsIgnoreCase("RERUN")) { // rerun the migration process
            properties.put(PROP_MIGRATION_SKIP_DOCUMENTS, "true");
            properties.put(PROP_MIGRATION_REPLACE_DOCUMENTS, "false");
            properties.put(PROP_MIGRATION_COPY_PERMISSIONS, "false");
            properties.put(PROP_TARGET_STRUCTURE_KEEP_ORIGINAL, "true");
        }*/
    }

    private static void setAspectList() {
        String aspectListString = get(PROP_TARGET_NEW_ASPECT_LIST);
        if(aspectListString != null && !aspectListString.isEmpty()) {
            String[] AspectArray = aspectListString.split(",");
            aspectList = new ArrayList<>(Arrays.asList(AspectArray));
        }
    }

    public static List<String> getAspectList() {
        return aspectList;
    }
}
