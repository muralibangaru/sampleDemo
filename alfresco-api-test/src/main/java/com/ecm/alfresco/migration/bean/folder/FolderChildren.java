package com.ecm.alfresco.migration.bean.folder;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;

import com.ecm.alfresco.migration.bean.document.DocumentItem;

import java.util.LinkedList;
import java.util.Queue;

public class FolderChildren {
    private Queue<DocumentItem> documentQueue = new LinkedList<>();
    private Queue<Folder> folderQueue = new LinkedList<>();
    private Queue<CmisObject> objectQueue = new LinkedList<>();

    public void addFolder(Folder folder) {
        folderQueue.add(folder);
    }

    public void addDocument(DocumentItem documentId) {
        documentQueue.add(documentId);
    }

    public void addObject(CmisObject object) {
        objectQueue.add(object);
    }

    public Queue<DocumentItem> getDocumentQueue() {
        return documentQueue;
    }

    public Queue<Folder> getFolderQueue() {
        return folderQueue;
    }

    public Queue<CmisObject> getObjectQueue() {
        return objectQueue;
    }
}
