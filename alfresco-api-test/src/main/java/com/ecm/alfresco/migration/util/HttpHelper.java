package com.ecm.alfresco.migration.util;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ecm.alfresco.migration.bean.access.AccessDetails;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * This class provides a wrapper over the http calls
 *
 * @author Miguel Sánchez
 */
public class HttpHelper {
    private static Log logger = LogFactory.getLog(HttpHelper.class);

    public static String get(AccessDetails accessDetails, String url, NameValuePair[] params, String cookie) {
        logParams(url, params);
        System.out.println("URL: GET " + url);
        String response = "";
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(url);
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
            httpClient.getParams().setAuthenticationPreemptive(true);
            Credentials credentials = new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        }
        if (params != null)
            getMethod.setQueryString(params);
        if (cookie != null)
            getMethod.setRequestHeader("Cookie", cookie);

        try {
            httpClient.executeMethod(getMethod);

            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                response = getMethod.getResponseBodyAsString();
                //System.out.println("Response: " + response);

            } else {
                response = "ERROR: " + getMethod.getStatusCode() + ", " + getMethod.getResponseBodyAsString();
                System.out.println("Response: " + response);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            //logPostResponseError(getMethod);
        }

        getMethod.releaseConnection();

        return response;
    }

    public static InputStream getStream(AccessDetails accessDetails, String url, NameValuePair[] params, String cookie) {
        logParams(url, params);
        System.out.println("URL: GET " + url);
        InputStream response = null;
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = new GetMethod(url);
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
            httpClient.getParams().setAuthenticationPreemptive(true);
            Credentials credentials = new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        }
        if (params != null)
            getMethod.setQueryString(params);
        if (cookie != null)
            getMethod.setRequestHeader("Cookie", cookie);

        try {
            httpClient.executeMethod(getMethod);

            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                response = getMethod.getResponseBodyAsStream();

            } else {
                logPostResponseError(getMethod);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            logPostResponseError(getMethod);
        }

        return response;
    }

    /**
     * Performs a POST request and returns the response as a String
     *
     * @param url    url to be requested
     * @return the response in plain format
     */
    public static String post(AccessDetails accessDetails, String url, String json) {
        //System.out.println("URL: POST " + url);
        String response = "";
        HttpClient httpClient = new HttpClient();
        PostMethod postMethod = new PostMethod(url);
        postMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
            httpClient.getParams().setAuthenticationPreemptive(true);
            Credentials credentials = new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, credentials);
        }

        if (json != null) {
            try {
                StringRequestEntity requestEntity = new StringRequestEntity(json,"application/json","UTF-8");
                postMethod.setRequestEntity(requestEntity);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }

        try {
            httpClient.executeMethod(postMethod);

            if (postMethod.getStatusCode() == HttpStatus.SC_OK) {
                response = postMethod.getResponseBodyAsString();

                //System.out.println("Response: " + response);

            } else {
                response = "ERROR: " + postMethod.getStatusCode() + ", " + postMethod.getResponseBodyAsString();
                System.out.println("ERROR: " + postMethod.getStatusCode() + ", " + postMethod.getResponseBodyAsString());
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            //logPostResponseError(postMethod);
        }

        postMethod.releaseConnection();
        logger.debug("Response: " + response);

        return response;
    }


    /**
     * Logs the error received from a POST request
     *
     */
    private static void logPostResponseError(PostMethod postMethod) {
        try {
            logger.error("Response STATUS: " + postMethod.getStatusCode());
            logger.error("Response: " + postMethod.getResponseBodyAsString());

        } catch (IOException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the error received from a POST request
     *
     * @param getMethod the object used to perform the POST request
     */
    private static void logPostResponseError(GetMethod getMethod) {
        try {
            logger.error("Response STATUS: " + getMethod.getStatusCode());
            logger.error("Response: " + getMethod.getResponseBodyAsString());

        } catch (IOException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the url and the params passed if debug is enabled
     *
     * @param url    absolute URL passed for a logging reference
     * @param params params passed for a logging reference
     */
    private static void logParams(String url, NameValuePair[] params) {
        if (logger.isDebugEnabled()) {
            logger.debug("URL: " + url);
            if (params != null) {
                for (NameValuePair oneParam : params) {
                    logger.debug("Param: " + oneParam.getName() + ", Value: " + oneParam.getValue());
                }
            }
        }
    }

    /**
     * Converts a value list into a value array
     *
     * @param paramList list of parameters to be converted
     * @return an array of values
     */
    public static NameValuePair[] getArrayNameValuePair(List<NameValuePair> paramList) {
        NameValuePair[] paramArray = new NameValuePair[paramList.size()];
        paramArray = paramList.toArray(paramArray);

        return paramArray;
    }


}
