package com.ecm.alfresco.migration.util;

/**
 * @author Miguel Sanchez
 * @version 1.0
 *          2016-09-30
 */
public class NodeRefUtil {
    /**
     * Removes version label from the node ID
     * @param nodeId
     * @return
     */
    static public String removeVersionLabel(String nodeId) {
        if (nodeId.contains(";"))
            return nodeId.substring(0, nodeId.lastIndexOf(';'));
        else
            return nodeId;
    }

    /**
     * Removes the workspace from the nodeRef
     * @param nodeRef
     * @return
     */
    static public String removeWorkSpace(String nodeRef) {
        if(nodeRef != null)
            return nodeRef.substring(nodeRef.lastIndexOf("/") + 1);
        else
            return "";
    }
}
