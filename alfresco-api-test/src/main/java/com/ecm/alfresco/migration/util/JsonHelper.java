package com.ecm.alfresco.migration.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * This class provides some JSON utilities
 *
 * @author Miguel Sánchez
 */
public class JsonHelper {
    private static Log logger = LogFactory.getLog(JsonHelper.class);

    /**
     * Creates a JSON object form a plain text
     *
     * @param plainText JSON in plain text
     * @return a JSON object
     */
    public static JSONObject getJSONObject(String plainText) {
        JSONObject object = null;
        try {
            object = new JSONObject(new JSONTokener(plainText));

        } catch (JSONException e) {
            logger.error(e.getMessage());
            throw e;
        }

        return object;
    }

    /**
     * Creates a JSON array form a plain text
     *
     * @param plainText JSON in plain text
     * @return a JSON array
     */
    public static JSONArray getJSONArray(String plainText) {
        JSONArray array = null;

        try {
            array = new JSONArray(new JSONTokener(plainText));

        } catch (JSONException e) {
            logger.error(e.getMessage());
            throw e;
        }

        return array;
    }
}