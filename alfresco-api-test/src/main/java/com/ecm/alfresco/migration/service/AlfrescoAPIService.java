package com.ecm.alfresco.migration.service;

import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.bean.document.DocumentProperties;
import com.ecm.alfresco.migration.job.config.MigrationProperties;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.util.*;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

public class AlfrescoAPIService {
    @Autowired
    private JobParameters jobParameters;
    private static final Logger logger = Logger.getLogger(AlfrescoAPIService.class);
    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final String[] REMOVE_PROPERTIES_LIST = {"{http://www.alfresco.org/model/content/1.0}content",
            "{http://www.alfresco.org/model/system/1.0}store-identifier",
            "{http://www.alfresco.org/model/versionstore/2.0}frozenModifier",
            "{http://www.alfresco.org/model/content/1.0}initialVersion",
            "{http://www.alfresco.org/model/content/1.0}versionType",
            "{http://www.alfresco.org/model/versionstore/2.0}frozenCreated",
            "{http://www.alfresco.org/model/versionstore/2.0}frozenNodeDbId",
            "{http://www.alfresco.org/model/versionstore/2.0}frozenModified",
            "{http://www.alfresco.org/model/versionstore/2.0}frozenNodeRef",
            "{http://www.alfresco.org/model/versionstore/2.0}metadata-versionType",
            "{http://www.alfresco.org/model/versionstore/2.0}frozenCreator",
            "{http://www.alfresco.org/model/system/1.0}store-protocol",
            "{http://www.alfresco.org/model/system/1.0}node-dbid",
            "{http://www.alfresco.org/model/system/1.0}locale",
            "{http://www.alfresco.org/model/versionstore/2.0}versionLabel",
           /* "{http://www.alfresco.org/model/content/1.0}modifier",*/
            "{http://www.alfresco.org/model/versionstore/2.0}frozenAccessed",
            "{http://www.alfresco.org/model/versionstore/2.0}versionDescription",
            "{http://www.alfresco.org/model/system/1.0}node-uuid",
            "{http://www.alfresco.org/model/content/1.0}versionLabel",
            "{http://www.alfresco.org/model/system/1.0}node-uuid",
            "{http://www.alfresco.org/model/content/1.0}lockType",
            "{http://www.alfresco.org/model/content/1.0}lockLifetime",
            "{http://www.alfresco.org/model/content/1.0}lockOwner",
            "{http://www.alfresco.org/model/content/1.0}lastThumbnailModification"};

    private static final String[] FALSE_PROPERTIES_LIST = {"{http://www.alfresco.org/model/content/1.0}autoVersion",
            "{http://www.alfresco.org/model/content/1.0}autoVersionOnUpdateProps"};

    /**
     * Gets version list in JSON array format
     * @param documentItem
     * @return
     * @throws Exception
     */
    public JSONArray getVersionArray(DocumentItem documentItem) throws Exception {
        String url = getHost(SOURCE) + "/alfresco/service/api/version?nodeRef=workspace://SpacesStore/" + documentItem.getSourceNodeRef();
        logger.debug("Get version URL: " + url);
        String result = HttpPoolHelper.get(jobParameters.getAccessDetailsSource(), url, null, null, SOURCE);
        logger.debug("Result: " + result);
        JSONArray versionArray = JsonHelper.getJSONArray(result);
        return versionArray;
    }

    /**
     * Gets version properties
     * @param versionNodeId
     * @param sourceNodeId
     * @return
     * @throws Exception
     */
    public DocumentProperties getVersionProperties(String versionNodeId, String sourceNodeId) throws Exception {
        TimeUtil.resume("PROPERTIES", jobParameters.getStopWatchGetProperties());
        String store = "version2Store";
        String nodeId = versionNodeId;

        if (versionNodeId == null) {
            store = "SpacesStore";
            nodeId = sourceNodeId;
        }

        String url = getHost(SOURCE) + "/alfresco/service/api/metadata?nodeRef=workspace://" + store + "/" + NodeRefUtil.removeWorkSpace(nodeId);
        logger.debug("Get version properties URL: " + url);
        String result = HttpPoolHelper.get(jobParameters.getAccessDetailsSource(), url, null, null, SOURCE);
        logger.debug("Properties BEFORE cleaning" + result);
        DocumentProperties documentProperties = cleanProperties(JsonHelper.getJSONObject(result), versionNodeId);
        JSONObject metadata = documentProperties.getDocumentProperties();
        logger.debug("Properties AFTER cleaning" + metadata.toString());
        TimeUtil.suspend("PROPERTIES", jobParameters.getStopWatchGetProperties());
        return documentProperties;
    }

    /**
     * Gets file for a specific version
     * @param sourceNodeId
     * @param versionNodeId
     * @param fileName
     * @param currentVersion
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public File getVersionFile(String sourceNodeId, String versionNodeId, String fileName, boolean currentVersion) throws IOException, URISyntaxException, InterruptedException {
        TimeUtil.resume("FILE", jobParameters.getStopWatchGetFile());
        String url; 
        boolean searchFromVersionStore = false;
        
        if (versionNodeId != null && !currentVersion) {
			searchFromVersionStore = true;
		}
        url = getHost(SOURCE) + "/alfresco/service/api/node/workspace/" + getStore(currentVersion) + "/" + (currentVersion ? sourceNodeId : versionNodeId) + "/content";

        logger.debug("Get Version File URL: " + url);
        InputStream in = HttpPoolHelper.getStream(jobParameters.getAccessDetailsSource(), url, null, null, SOURCE);
        if (in == null)
            throw new FileNotFoundException("Couldn't get file " + fileName + " from source repository");

        File documentFile = createTempFile(fileName, in);
        TimeUtil.suspend("FILE", jobParameters.getStopWatchGetFile());
        return documentFile;
    }

    /**
     * Uploads a document into the repository
     * @param documentItem
     * @param file
     * @return JSON status message
     * @throws Exception
     */
    public JSONObject upload(DocumentItem documentItem, File file) throws Exception {
        String response = null;

        try {

            String url = getHost(TARGET) + "/alfresco/service/api/upload";

            HttpEntity entity = getEntity(file, documentItem);
            logger.debug("Upload document URL: " + url);
            response = HttpPoolHelper.post(jobParameters.getAccessDetailsTarget(), url, null, null, entity, null, null, TARGET);
            logger.debug("API Upload response: " + response);
            JSONObject jsonResponse = JsonHelper.getJSONObject(response);

            return jsonResponse;

        } catch (JSONException e) {
            TimeUtil.suspend("UPLOAD", jobParameters.getStopWatchUpload());
            logger.error("Upload failed: " + documentItem.getFilename() + ", Response: " + response);
            throw new Exception("Upload Response: " + response + ", Version Label: " + documentItem.getVersionLabel());

        } catch (Exception e) {
            TimeUtil.suspend("UPLOAD", jobParameters.getStopWatchUpload());
            logger.error("Upload failed: " + documentItem.getFilename());

            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }

            throw e;
        }
    }

    /**
     * Gets an http entity based on a document
     * @param file
     * @param documentItem
     * @return
     * @throws Exception
     */
    private HttpEntity getEntity(File file, DocumentItem documentItem) throws Exception {
        Folder destinationFolder = getDestinationFolder(documentItem);
        String destinationNodeId = NodeRefUtil.removeWorkSpace(destinationFolder.getId());
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("filedata", file, ContentType.APPLICATION_OCTET_STREAM, documentItem.getFilename());
        builder.addTextBody("contenttype ", documentItem.getContentType(), ContentType.MULTIPART_FORM_DATA);

        if (documentItem.getTargetNodeRef() == null) {
            builder.addTextBody("destination", "workspace://SpacesStore/" + destinationNodeId, ContentType.TEXT_PLAIN);
            logger.debug("Document is the first version. Destination: workspace://SpacesStore/" + destinationNodeId);
            logger.debug("Entity -> filename: " + documentItem.getFilename() + ", contenttype: " + documentItem.getContentType() + ", targetNodeId: " + documentItem.getTargetNodeRef() + ", versionLabel: " + documentItem.getVersionLabel() + ", majorversion: false");

        } else {
            String targetNodeId = NodeRefUtil.removeVersionLabel(documentItem.getTargetNodeRef());
            builder.addTextBody("updateNodeRef", targetNodeId, ContentType.MULTIPART_FORM_DATA);
            logger.debug("Updating document with a new version: " + documentItem.getVersionLabel() + ", nodeId: " + targetNodeId);

            if (documentItem.getVersionLabel().endsWith(".0")) {
                builder.addTextBody("majorversion", "true", ContentType.MULTIPART_FORM_DATA);
                logger.debug("Document is a major version");
                logger.debug("Entity -> filename: " + documentItem.getFilename() + ", contenttype: " + documentItem.getContentType() + ", targetNodeId: " + targetNodeId + ", versionLabel: " + documentItem.getVersionLabel() + ", majorversion: true");

            } else {
                builder.addTextBody("majorversion", "false", ContentType.MULTIPART_FORM_DATA);
                logger.debug("Entity -> filename: " + documentItem.getFilename() + ", contenttype: " + documentItem.getContentType() + ", targetNodeId: " + targetNodeId + ", versionLabel: " + documentItem.getVersionLabel() + ", majorversion: false");
            }
        }

        return builder.build();
    }

    /**
     * Gets destination folder for a specific document
     * @param documentItem
     * @return
     */
    private Folder getDestinationFolder(DocumentItem documentItem) {
        Folder destinationFolder = CmisHelper.createFolder(jobParameters.getSessionTarget(), documentItem.getTargetDestinationFolder(), new HashMap<String, Object>());
        if (destinationFolder == null) {
            TimeUtil.suspend("UPLOAD", jobParameters.getStopWatchUpload());
            throw new RuntimeException("Unable to create folder " + documentItem.getTargetDestinationFolder());
        }

        return destinationFolder;
    }

    /**
     * Updates a node's properties
     * @param documentItem
     * @param documentProperties
     * @throws Exception
     */
    public void updateNodeProperties(DocumentItem documentItem, DocumentProperties documentProperties) throws Exception {
        String targetNodeId = NodeRefUtil.removeWorkSpace(documentItem.getTargetNodeRef());
        String response = null;

        try {
            String url = getHost(TARGET) + "/alfresco/service/api/metadata/node/workspace/SpacesStore/" + NodeRefUtil.removeVersionLabel(targetNodeId);
            logger.debug("Update document properties URL: " + url);
            response = HttpPoolHelper.post(jobParameters.getAccessDetailsTarget(), url, null, null, null, null, documentProperties.getDocumentProperties().toString(), TARGET);
            logger.debug("API Update properties response: " + response);
            JsonHelper.getJSONObject(response); //verify if the response is a JSON

        } catch (JSONException e) {
            TimeUtil.suspend("UPLOAD", jobParameters.getStopWatchUpload());
            logger.error("Update Node failed: " + targetNodeId + ", Response: " + response);
            throw new Exception("Update Node Response: " + response);
        }
    }

    /**
     * Sets node permissions
     * @param sourceNodeId
     * @param targetNodeId
     * @param jobParameters
     * @param folderPath
     * @throws Exception
     */
    public void setNodePermissions(String sourceNodeId, String targetNodeId, JobParameters jobParameters, String folderPath) throws Exception {
        try {
            JSONObject sourcePermissions = getNodePermissions(sourceNodeId, jobParameters);
            JSONObject targetPermissions = getTargetPermissions(sourcePermissions);
            String url = getHost(TARGET) + "/alfresco/service/slingshot/doclib/permissions/workspace/SpacesStore/" + NodeRefUtil.removeWorkSpace(targetNodeId);
            logger.debug("POST node permission URL: " + url);
            String response = HttpPoolHelper.post(jobParameters.getAccessDetailsTarget(), url, null, null, null, null, targetPermissions.toString(),TARGET);
            logger.debug("API set node permissions response: " + response);
            JsonHelper.getJSONObject(response); // create a json from the response to verify if the response is a JSON

            if(((JSONArray)(targetPermissions.get("permissions"))).length() > 0)
                logger.info("Permissions set for: " + folderPath + ",  JSON: " + targetPermissions.toString());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Gets node permissions
     * @param nodeRef
     * @param jobParameters
     * @return
     * @throws Exception
     */
    public JSONObject getNodePermissions(String nodeRef, JobParameters jobParameters) throws Exception {
        try {
            String url = getHost(SOURCE) + "/alfresco/service/slingshot/doclib/permissions/workspace/SpacesStore/" + NodeRefUtil.removeWorkSpace(nodeRef);
            logger.debug("GET node permission URL: " + url);
            String response = HttpPoolHelper.get(jobParameters.getAccessDetailsSource(), url, null, null, SOURCE);
            logger.debug("API get node permissions response: " + response);
            return JsonHelper.getJSONObject(response);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Gets a clean list of permissions in JSON format
     * @param sourcePermissions
     * @return
     */
    private JSONObject getTargetPermissions(JSONObject sourcePermissions) {
        JSONObject targetPermissions = new JSONObject();
        JSONArray targetPermissionsArray = new JSONArray();
        boolean isInherited = (boolean) sourcePermissions.get("isInherited");
        JSONArray directArray = (JSONArray) sourcePermissions.get("direct");

        for (int i = 0; i < directArray.length(); i++) {
            JSONObject onePermission = directArray.getJSONObject(i);
            String role = (String) onePermission.get("role");
            JSONObject authorityObject = (JSONObject) onePermission.get("authority");
            String authorityName = (String) authorityObject.get("name");

            JSONObject oneTargetPermission = new JSONObject("{\"authority\":\"" + authorityName + "\", \"role\": \"" + role + "\", \"remove\": false}");
            targetPermissionsArray.put(oneTargetPermission);
        }

        targetPermissions.put("permissions", targetPermissionsArray);
        targetPermissions.put("isInherited", isInherited);

        logger.debug("Source Permissions: " + directArray + ", Target Permissions: " + targetPermissions.toString());

        return targetPermissions;
    }

    /**
     * Creates a temporary file
     * @param fileName
     * @param in
     * @return
     * @throws IOException
     */
    private File createTempFile(String fileName, InputStream in) throws IOException {
        File documentFile = File.createTempFile(fileName, "");
        OutputStream out = new FileOutputStream(documentFile);
        IOUtils.copy(in, out);
        out.close();
        return documentFile;
    }

    /**
     * Cleans metadata properties, removes unnecessary entries, adds extra information
     * @param metadata
     * @param versionNodeId
     * @return
     * @throws Exception
     */
    private DocumentProperties cleanProperties(JSONObject metadata, String versionNodeId) throws Exception {
        if (metadata.isNull("properties"))
            throw new Exception("Metadata is empty: " + metadata + ", version node ID: " + versionNodeId);

        String nodeRef = (String) metadata.get("nodeRef");

        // remove unnecessary properties
        metadata.remove("nodeRef");
        JSONObject properties = (JSONObject) metadata.get("properties");
        for (String oneProperty : REMOVE_PROPERTIES_LIST) {
            properties.remove(oneProperty);
        }

        // remove specific properties set in migration.properties
        for (String oneProperty : jobParameters.getPropertyFilter()) {
            properties.remove(oneProperty);
        }

        // set to false properties
        for (String oneProperty : FALSE_PROPERTIES_LIST) {
            properties.put(oneProperty, false);
        }

        // remove null properties
        DocumentProperties documentProperties = new DocumentProperties();
        for (Iterator<String> it = properties.keys(); it.hasNext(); ) {
            String oneProperty = it.next();
            if (properties.isNull(oneProperty)) {
                it.remove();

            } else { //capture collections and empty the value to avoid issues with the API
                Object onePropertyValue = properties.get(oneProperty);
                if (onePropertyValue instanceof JSONArray) {
                    documentProperties.addList(oneProperty, (JSONArray) onePropertyValue, jobParameters);
                    properties.put(oneProperty, "");
                }
            }
        }

        //change content type
        String newContentType = MigrationProperties.get(MigrationProperties.PROP_TARGET_NEW_CONTENT_TYPE);

        if (newContentType != null && newContentType.contains(":")) {
            metadata.put("type", newContentType);
        }

        //add nodeRef in a different property
        if(Boolean.valueOf(MigrationProperties.get(MigrationProperties.PROP_TARGET_KEEP_SOURCE_NODEID_ENABLED))){
            properties.put(MigrationProperties.get(MigrationProperties.PROP_TARGET_KEEP_SOURCE_NODEID_PROPERTY), nodeRef);
        }

        //add new aspects
        if(MigrationProperties.getAspectList() != null && !MigrationProperties.getAspectList().isEmpty()){
            JSONArray aspects = (JSONArray) metadata.get("aspects");
            for (String oneAspect : MigrationProperties.getAspectList()) {
                aspects.put(oneAspect);
            }

            metadata.put("aspects", aspects);
        }

        metadata.put("properties", properties);
        documentProperties.setDocumentProperties(metadata);

        return documentProperties;
    }

    /**
     * Gets store type based on the version
     * @param currentVersion
     * @return
     */
    private String getStore(boolean currentVersion) {
        if (currentVersion)
            return "SpacesStore";
        else
            return "version2Store";
    }

    /**
     * Gets the host url based on the repository (SOURCE or TARGET)
     * @param type
     * @return
     */
    private String getHost(String type) {
        return MigrationProperties.get(type + "." + MigrationProperties.PROP_ALFRESCO_HOST_URL);
    }

    /**
     * Gets a list from csv string
     * @param list
     * @return
     */
    public static ArrayList<String> getList(String list) {
        String[] fileNameArray = list.split(",");
        return new ArrayList<>(Arrays.asList(fileNameArray));
    }
}
