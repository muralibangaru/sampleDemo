package com.ecm.alfresco.migration.bean.document;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ecm.alfresco.migration.job.param.JobParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentProperties {
    private Map<String, List<String>> collectionProperties = new HashMap<>();
    private JSONObject documentProperties;
    private static final Logger logger = Logger.getLogger(DocumentProperties.class);

    public void addList(String property, JSONArray array, JobParameters jobParameters) {
        if (array != null && array.length() > 0) {
            String[] namespaceProperty = property.split("}");
            String prefix = jobParameters.getNamespacePrefixMap().get(namespaceProperty[0].replace("{", ""));
            List<String> list = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            String prefixProperty = prefix + ":" + namespaceProperty[1];
            collectionProperties.put(prefixProperty, list);
            logger.trace("Added collection: " + prefixProperty + ", Size: " + list.size());
        }
    }

    public Map<String, List<String>> getCollectionProperties() {
        return collectionProperties;
    }

    public void setCollectionProperties(Map<String, List<String>> collectionProperties) {
        this.collectionProperties = collectionProperties;
    }

    public JSONObject getDocumentProperties() {
        return documentProperties;
    }

    public void setDocumentProperties(JSONObject documentProperties) {
        this.documentProperties = documentProperties;
    }


}
