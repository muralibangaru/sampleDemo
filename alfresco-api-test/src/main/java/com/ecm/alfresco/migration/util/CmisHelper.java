package com.ecm.alfresco.migration.util;

import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.bean.folder.FolderChildren;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.log4j.Logger;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.util.*;

/**
 * This class provides a wrapper over CMIS
 *
 * @author Miguel Sanchez
 */
public class CmisHelper {
    private static Logger logger = Logger.getLogger(CmisHelper.class);

    /**
     * creates a session using regular credentials, username and password
     *
     * @param hostUrl  alfresco URL domain
     * @param user     username
     * @param password password
     * @return session
     */
    public static Session getSession(String hostUrl, String user, String password, String cmisUrl) {
        Map<String, String> parameter = getBasicParameter(hostUrl, cmisUrl);
        parameter.put(SessionParameter.USER, user);
        parameter.put(SessionParameter.PASSWORD, password);

        return getFactorySession(parameter);
    }

    /**
     * Creates a document
     *
     * @param session    active session
     * @param file       document to be stored
     * @param properties document properties
     * @return the created document
     */
    public static Document createDocument(Session session, Folder folder, File file, Map<String, Object> properties) throws FileNotFoundException {
        validateParameters(folder, file);
        InputStream inputStream = new FileInputStream(file);
        Document document = null;

        try {
            logMapDebug(properties, "Document properties " + properties.get(PropertyIds.NAME));
            String mimeType = new MimetypesFileTypeMap().getContentType(file);
            ContentStream contentStream = session.getObjectFactory().createContentStream((String) properties.get(PropertyIds.NAME), file.length(), mimeType, inputStream);
            document = folder.createDocument(properties, contentStream, VersioningState.MAJOR);

        } catch (CmisContentAlreadyExistsException e) {
            try {
                inputStream.close();
                throw new CmisContentAlreadyExistsException();

            } catch (IOException ex) {
                logger.error("Error closing input stream from file: " + properties.get(PropertyIds.NAME) + ", " + ex.getMessage());
            }

        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return document;

    }

    /**
     * Verifies if file or folder is null
     * @param folder
     * @param file
     */
    private static void validateParameters(Folder folder, File file) {
        if (folder == null)
            throw new IllegalArgumentException("Destination folder is null");

        if (file == null)
            throw new IllegalArgumentException("File to upload is null");
    }

    /**
     * retrieves a document by filename and path
     *
     * @param session      active session
     * @param folderPath   parent folder path that contains the document
     * @param documentName document name
     * @return the document if it's found and null if doesn't exist
     */
    public static Document getDocumentByPath(Session session, String folderPath, String documentName) {
        return (Document) session.getObjectByPath(folderPath + "/" + documentName);
    }

    /**
     * creates a folder
     *
     * @param session    active session
     * @param folderPath the new folder path
     * @param properties folder properties
     * @return the newly folder created
     */
    public static Folder createFolder(Session session, String folderPath, Map<String, Object> properties) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        try {
            Folder folder = getFolder(session, folderPath);
            logger.debug("Folder already exists: " + folderPath);

            return folder;

        } catch (CmisObjectNotFoundException ex) {
            try {
                logger.debug("Folder does not exists: '" + folderPath + "'");
                String parentFolderPath = getParentFolderPath(folderPath);
                Folder parentFolder = createFolder(session, parentFolderPath, properties);
                String folderName = getFolderNameFromPath(folderPath);

                properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
                properties.put(PropertyIds.NAME, folderName);

                Folder folder = null;
                if (parentFolder != null) {
                    folder = parentFolder.createFolder(properties);
                    logger.info("New folder created: " + folderPath);

                } else {
                    logger.debug("Folder does not exists: " + folderPath);
                }

                return folder;

            } catch (CmisContentAlreadyExistsException e) {
                logger.debug("Folder already exists, it was created before by another thread: " + folderPath);
                return getFolder(session, folderPath);

            } catch (CmisConstraintException exc) {
                logger.error(exc.getMessage());
                return null;

            } catch (Exception exception) {
                exception.printStackTrace();
                logger.error("Folder does not exists: " + folderPath + ", error: " + exception.getMessage());
                return null;

            }
        }
    }

    /**
     * Gets document library folder
     * @param session
     * @param siteId
     * @return
     */
    public static Folder getDocumentLibrary(Session session, String siteId) {
        return (Folder) session.getObjectByPath(getSiteFolderPath(siteId, ""));
    }

    /**
     * Gets folder's parent folder path
     * @param folderPath
     * @return
     */
    private static String getParentFolderPath(String folderPath) {
        if (folderPath != null && folderPath.contains("/")) {
            logger.debug("FolderPath: " + folderPath + ", ParentFolder: " + folderPath.substring(0, folderPath.lastIndexOf('/')));
            String parentFolder = folderPath.substring(0, folderPath.lastIndexOf('/'));
            if(parentFolder.isEmpty())
                return "/";
            else
                return parentFolder;

        } else {
            return folderPath;
        }

    }

    /**
     * Gets folder name from its path
     * @param folderPath
     * @return
     */
    private static String getFolderNameFromPath(String folderPath) {
        if (folderPath != null && folderPath.contains("/") && folderPath.lastIndexOf('/') != folderPath.length() - 1)
            return folderPath.substring(folderPath.lastIndexOf('/') + 1);
        else
            return "";
    }

    /**
     * retrieves a folder
     *
     * @param session    active session
     * @param siteId     site ID that holds the folder
     * @param folderPath folder path
     * @return a folder
     */
    public static Folder getFolder(Session session, String siteId, String folderPath) {
        logger.debug("Retrieving folder " + getSiteFolderPath(siteId, folderPath));
        return (Folder) session.getObjectByPath(getSiteFolderPath(siteId, folderPath));
    }

    /**
     * Gets a folder based on its path
     * @param session
     * @param folderPath
     * @return
     */
    public static Folder getFolder(Session session, String folderPath) {
        logger.debug("Retrieving folder " + folderPath);
        return (Folder) session.getObjectByPath(folderPath);
    }

    /**
     * Gets documents by their folder path
     * @param session
     * @param siteId
     * @param folderPath
     * @return
     */
    public static List<CmisObject> getDocumentsByFolderPath(Session session, String siteId, String folderPath) {
        List<CmisObject> documentList = new ArrayList<>();
        Folder folder = getFolder(session, siteId, folderPath);
        Iterator<CmisObject> iterator = folder.getChildren().iterator();

        while (iterator.hasNext()) {
            CmisObject object = iterator.next();
            if (object.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
                Folder subFolder = (Folder) object;
                documentList.add(object);
                documentList.addAll(getDocumentsByFolderPath(session, siteId, subFolder.getPath()));

            } else if (object.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
                documentList.add(object);

            }
        }

        return documentList;
    }

    /**
     * Gets a page of documents by its folder path
     * @param session
     * @param folderPath
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public static ItemIterable<CmisObject> getDocumentsByFolderPath(Session session, String folderPath, int pageNumber, int pageSize) {
        Folder folder = getFolder(session, folderPath);
        return folder.getChildren().skipTo(pageNumber * pageSize).getPage(pageSize);
    }

    /**
     * Get a folder's children
     * @param session
     * @param folderPath
     * @param pageNumber
     * @param pageSize
     * @param skipDocuments
     * @return
     */
    public static FolderChildren getFolderChildren(Session session, String folderPath, int pageNumber, int pageSize, boolean skipDocuments) {
        ItemIterable<CmisObject> onePage = getDocumentsByFolderPath(session, folderPath, pageNumber, pageSize);
        FolderChildren folderChildren = new FolderChildren();

        while (onePage.iterator().hasNext()) {
            CmisObject object = onePage.iterator().next();
            if (object.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
                folderChildren.addFolder((Folder) object);

            } else if (!skipDocuments && object.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
                DocumentItem documentItem = new DocumentItem((Document) object, folderPath);

                if(!documentItem.getDocumentId().endsWith(";pwc")) //verify if it's not a working copy
                    folderChildren.addDocument(documentItem);

            } else if (!skipDocuments) {
                folderChildren.addObject(object);
            }
        }

        return folderChildren;
    }


    /**
     * concatenates the information to return the site folder path
     *
     * @param siteId     site ID that holds the document or folder
     * @param folderPath folder path inside of the site
     * @return the folder path at the repository level
     */
    private static String getSiteFolderPath(String siteId, String folderPath) {
        return "/Sites/" + siteId + "/documentLibrary" + folderPath;
    }

    /**
     * sets the basic paramenters
     *
     * @param hostUrl Alfresco URL domain
     * @return basic parameters
     */
    private static Map<String, String> getBasicParameter(String hostUrl, String cmisUrl) {
        Map<String, String> parameter = new HashMap<String, String>();
        parameter.put(SessionParameter.ATOMPUB_URL, hostUrl + cmisUrl);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        logger.debug("URL: " + hostUrl + cmisUrl);
        return parameter;
    }

    /**
     * creates a session
     *
     * @param parameter factory parameters
     * @return an active session
     */
    private static Session getFactorySession(Map<String, String> parameter) {
        SessionFactory factory = SessionFactoryImpl.newInstance();
        List<Repository> repositories = factory.getRepositories(parameter);

        return repositories.get(0).createSession();
    }

    /**
     * retrieves a collection of documents
     *
     * @param session active session
     * @param query   CMIS query
     */
    public static ItemIterable<QueryResult> getDocuments(Session session, String query) {
        return session.query(query, false);
    }

    /**
     * retrieves a collection of documents
     *
     * @param session    active session
     * @param query      CMIS query
     * @param pageNumber page number
     * @param pageSize   @return a collection of documents
     */
    public static ItemIterable<QueryResult> getDocuments(Session session, String query, int pageNumber, int pageSize) {
        return session.query(query, false).skipTo(pageNumber * pageSize).getPage(pageSize);
    }


    /**
     * retrieves total amount of documents from a given query
     *
     * @param session active session
     * @param query   CMIS query
     */
    public static long countDocuments(Session session, String query) {
        // set maxItems to zero to avoid the transmission of the documents but keep the total num items on the result
        return session.query(query, false).getPage(0).getTotalNumItems();
    }

    /**
     * retrieves a document by object ID
     *
     * @param session  active session
     * @param objectId document object ID
     * @return the document found and null if it doesn't exist any
     */
    public static void downloadDocument(Session session, String objectId, OutputStream out) {
        Document document = (Document) session.getObject(objectId);
        downloadDocument(document, out);
    }

    /**
     * retrieves a document by object ID
     *
     * @return the document found and null if it doesn't exist any
     */
    public static void downloadDocument(Document document, OutputStream out) {
        if (document != null) {
            try {
                ContentStream content = document.getContentStream();
                InputStream in = content.getStream();
                IOUtils.copy(in, out);
                out.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.debug("Document does not exist: " + document.getId());
        }
    }

    /**
     * Gets a document by its filename
     * @param session
     * @param type
     * @param fileName
     * @return
     */
    public static Document getDocumentByName(Session session, String type, String fileName) {
        String query = "SELECT * FROM " + type + " WHERE cmis:name = '" + fileName + "'";
        logger.info(query);
        session.query(query, false);
        ItemIterable<QueryResult> queryResult = session.query(query, false);
        for (QueryResult item : queryResult) {
            PropertyData<Object> nodeRefProperty = item.getPropertyById("cmis:objectId");
            String nodeRef = (String) nodeRefProperty.getFirstValue();
            return (Document) session.getObject(nodeRef);
        }

        return null;
    }

    /**
     * Moves a document to a specific folder
     * @param session
     * @param objectId
     * @param destinationFolder
     */
    public static void moveDocument(Session session, String objectId, String destinationFolder) {
        try {
            Document document = (Document) session.getObject(objectId);
            Folder sourceFolder = document.getParents().get(0);
            Folder targetFolder = getFolder(session, destinationFolder);
            ObjectId sourceNodeId = new ObjectIdImpl((String) sourceFolder.getProperty(PropertyIds.OBJECT_ID).getFirstValue());
            ObjectId targetNodeId = new ObjectIdImpl((String) targetFolder.getProperty(PropertyIds.OBJECT_ID).getFirstValue());
            document.move(sourceNodeId, targetNodeId);

        } catch (CmisObjectNotFoundException e) {
            logger.info("Folder does not exist: " + destinationFolder);
            Folder newFolder = createFolder(session, destinationFolder, null);

            if(newFolder != null && newFolder.getId() != null)
                moveDocument(session, objectId, destinationFolder);
        }

    }

    /**
     * Deletes a document
     * @param session
     * @param objectId
     */
    public static void deleteDocument(Session session, String objectId) {
        Document document = (Document) session.getObject(objectId);
        document.delete();
    }

    /**
     * Logs keys and values from a map
     *
     * @param map     map
     * @param mapName map name to print in log
     */
    public static void logMapDebug(Map map, String mapName) {
        for (Object oneKey : map.keySet()) {
            Object oneObject = map.get(oneKey);
            if (String.class.isInstance(oneObject)) {
                logger.debug(mapName + ", Key: " + oneKey + ", Value: " + oneObject);
            } else {
                logger.debug(mapName + ", Key: " + oneKey + ", Value (not String): " + oneObject);
            }
        }
    }
}