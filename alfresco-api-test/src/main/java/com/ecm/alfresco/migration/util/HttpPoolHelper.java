package com.ecm.alfresco.migration.util;

import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.ecm.alfresco.migration.bean.access.AccessDetails;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a wrapper over the http calls
 *
 * @author Miguel Sanchez
 */
public class HttpPoolHelper {
    private static PoolingHttpClientConnectionManager poolHttpClientSource;
    private static PoolingHttpClientConnectionManager poolHttpClientTarget;
    private static CloseableHttpClient httpClientSource;
    private static CloseableHttpClient httpClientTarget;
    private static IdleConnectionMonitorThread staleMonitorSource;
    private static IdleConnectionMonitorThread staleMonitorTarget;
    private static final Logger logger = Logger.getLogger(HttpPoolHelper.class);

    /**
     * Closes all HTTP connections
     */
    public static void closeAllConnections() {
        closeAllConnectionsTarget();
        closeAllConnectionsSource();
        closeWaitingThreads();
    }

    /**
     * Closes all source HTTP connections
     */
    public static void closeAllConnectionsSource() {
        if (poolHttpClientSource != null && httpClientSource != null) {
            try {
                httpClientSource.close();
                poolHttpClientSource.close();
                staleMonitorSource.shutdown();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes all target HTTP connections
     */
    public static void closeAllConnectionsTarget() {
        if (poolHttpClientTarget != null && httpClientTarget != null) {
            try {
                httpClientTarget.close();
                poolHttpClientTarget.close();
                staleMonitorTarget.shutdown();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets an HTTP client
     * @param credentialsProvider
     * @param type
     * @return
     * @throws InterruptedException
     */
    public static CloseableHttpClient getHttpClient(CredentialsProvider credentialsProvider, String type) throws InterruptedException {
        switch (type) {
            case "source":
                return getHttpClientSource(credentialsProvider);

            case "target":
                return getHttpClientTarget(credentialsProvider);

            default:
                return null;
        }
    }

    /**
     * Gets an HTTP client for source repository
     * @param credentialsProvider
     * @return
     * @throws InterruptedException
     */
    public static CloseableHttpClient getHttpClientSource(CredentialsProvider credentialsProvider) throws InterruptedException {
        if (poolHttpClientSource == null) {
            poolHttpClientSource = new PoolingHttpClientConnectionManager();
            poolHttpClientSource.setDefaultMaxPerRoute(16);
            poolHttpClientSource.setMaxTotal(16);

            staleMonitorSource = new IdleConnectionMonitorThread(poolHttpClientSource);
            staleMonitorSource.start();

            try {
                staleMonitorSource.join(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                throw e;
            }
        }

        if (httpClientSource == null) {
            if (credentialsProvider != null)
                httpClientSource = HttpClients.custom().setConnectionManager(poolHttpClientSource).setDefaultCredentialsProvider(credentialsProvider).build();
            else
                httpClientSource = HttpClients.custom().setConnectionManager(poolHttpClientSource).build();
        }

        return httpClientSource;
    }

    /**
     * Gets an HTTP client for target repository
     * @param credentialsProvider
     * @return
     * @throws InterruptedException
     */
    public static CloseableHttpClient getHttpClientTarget(CredentialsProvider credentialsProvider) throws InterruptedException {
        if (poolHttpClientTarget == null) {
            poolHttpClientTarget = new PoolingHttpClientConnectionManager();
            poolHttpClientTarget.setDefaultMaxPerRoute(16);
            poolHttpClientTarget.setMaxTotal(16);

            staleMonitorTarget = new IdleConnectionMonitorThread(poolHttpClientTarget);
            staleMonitorTarget.start();

            try {
                staleMonitorTarget.join(1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                throw e;
            }
        }

        if (httpClientTarget == null) {
            if (credentialsProvider != null)
                httpClientTarget = HttpClients.custom().setConnectionManager(poolHttpClientTarget).setDefaultCredentialsProvider(credentialsProvider).build();
            else
                httpClientTarget = HttpClients.custom().setConnectionManager(poolHttpClientTarget).build();
        }

        return httpClientTarget;
    }

    /**
     * Gets a url
     * @param url
     * @param params
     * @return
     * @throws URISyntaxException
     */
    private static URI getURL(String url, List<NameValuePair> params) throws URISyntaxException {
        URIBuilder builder;
        URI uri;

        try {
            builder = new URIBuilder(url);

            if (params != null)
                for (NameValuePair oneParam : params) {
                    builder.setParameter(oneParam.getName(), oneParam.getValue());
                }

            uri = builder.build();

        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            throw e;
        }

        return uri;
    }


    /**
     * Performs a GET request and returns the response as a String
     *
     * @param accessDetails access credentials and service URL
     * @param url           url to be requested
     * @param params        request parameters
     * @param cookie        cookie to be sent on the header
     * @return the response in plain format
     */
    public static String get(AccessDetails accessDetails, String url, List<NameValuePair> params, String cookie, String type) throws Exception {
        logParams(url, params);
        String responseString = "";
        CredentialsProvider credentialsProvider = null;
        URI uri = getURL(url, params);
        HttpGet getMethod = new HttpGet(uri);
        CloseableHttpClient client = null;

        if (cookie != null)
            getMethod.setHeader("Cookie", cookie);

        try {
            if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword()));
            }

            client = getHttpClient(credentialsProvider, type);
            if (client == null) {
                throw new Exception("Http Client is NULL, Type: " + type + ", Credentials: " + credentialsProvider);
            }

            HttpResponse response = client.execute(getMethod);

            if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                responseString = EntityUtils.toString(response.getEntity());
                logger.trace("Response: " + responseString);

            } else {
                logger.error("URL GET:" + url + ", ERROR: " + response.getStatusLine().getStatusCode() + ", " + EntityUtils.toString(response.getEntity()));
            }

        } catch (Exception e) {
            logger.error("EXCEPTION URL GET:" + url + ", exception message: " + e.getMessage());
            throw e;
        }

        try {
            getMethod.releaseConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return responseString;
    }

    /**
     * Performs a GET request and returns the response as a stream
     *
     * @param accessDetails
     * @param url
     * @param params
     * @param cookie
     * @return
     */
    public static InputStream getStream(AccessDetails accessDetails, String url, List<NameValuePair> params, String cookie, String type) throws URISyntaxException, IOException, InterruptedException {
        logParams(url, params);
        InputStream responseStream = null;
        String responseString = "";
        CredentialsProvider credentialsProvider = null;
        URI uri = getURL(url, params);
        HttpGet getMethod = new HttpGet(uri);

        if (cookie != null)
            getMethod.setHeader("Cookie", cookie);

        try {
            if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword()));
            }

            HttpResponse response = getHttpClient(credentialsProvider, type).execute(getMethod);

            if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                responseStream = response.getEntity().getContent();

            } else {
                logger.error("URL GET STREAM:" + url + ", ERROR: " + response.getStatusLine().getStatusCode() + ", " + EntityUtils.toString(response.getEntity()));
            }

        } catch (Exception e) {
            logger.error("EXCEPTION URL GET STREAM:" + url + ", exception message: " + e.getMessage());
            throw e;
        }

        //getMethod.releaseConnection();

        return responseStream;
    }

    /**
     * Performs a POST request and returns the response as a String
     *
     * @param accessDetails   access credentials and service URL
     * @param url             url to be requested
     * @param params          request parameters
     * @param headerParams    request header parameters
     * @param multipartEntity file parts
     * @param cookie          cookie to be sent on the header
     * @param json            json object to be sent
     * @return the response in plain format
     */
    public static String post(AccessDetails accessDetails, String url, List<NameValuePair> params, List<NameValuePair> headerParams, HttpEntity multipartEntity, String cookie, String json, String type) throws URISyntaxException, IOException, InterruptedException {
        String responseString = "";
        logParams(url, params);
        CredentialsProvider credentialsProvider = null;
        URI uri = getURL(url, params);
        HttpPost postMethod = new HttpPost(uri);

        if (multipartEntity != null)
            postMethod.setEntity(multipartEntity);
        if (params != null)
            try {
                postMethod.setEntity(new UrlEncodedFormEntity(params));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        if (cookie != null)
            postMethod.setHeader("Cookie", cookie);

        if (json != null) {
            logger.trace("JSON: " + json);
            StringEntity requestEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            postMethod.setEntity(requestEntity);
        }

        if (headerParams != null) {
            for (NameValuePair oneParam : headerParams) {
                postMethod.setHeader(oneParam.getName(), oneParam.getValue());
            }
        }
        CloseableHttpResponse response;
        try {
            if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword()));
            }

            response = getHttpClient(credentialsProvider, type).execute(postMethod);

            if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                responseString = EntityUtils.toString(response.getEntity());

            } else {
                logger.error("URL POST:" + postMethod.getURI() + ", ERROR: " + response.getStatusLine().getStatusCode() + ", " + EntityUtils.toString(response.getEntity()));
                logger.error("Entity: " + postMethod.getEntity().toString());
            }

        } catch (Exception e) {
            logger.error("EXCEPTION URL POST:" + url + ", exception message: " + e.getMessage());
            throw e;
        }

        postMethod.releaseConnection();
        logger.trace("Response: " + responseString);

        return responseString;
    }

    /**
     * Sends an HTTP delete request
     * @param accessDetails
     * @param url
     * @param type
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public static String delete(AccessDetails accessDetails, String url, String type) throws URISyntaxException, IOException, InterruptedException {
        String responseString = "";
        CredentialsProvider credentialsProvider = null;
        URI uri = getURL(url, null);
        HttpDelete deleteMethod = new HttpDelete(uri);
        CloseableHttpResponse response;

        try {
            if (accessDetails.getUser() != null && accessDetails.getPassword() != null) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(accessDetails.getUser(), accessDetails.getPassword()));
            }

            response = getHttpClient(credentialsProvider, type).execute(deleteMethod);

            if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                responseString = EntityUtils.toString(response.getEntity());

            } else {
                logger.error("URL DELETE:" + deleteMethod.getURI() + ", ERROR: " + response.getStatusLine().getStatusCode() + ", " + EntityUtils.toString(response.getEntity()));
                ;
            }

        } catch (Exception e) {
            logger.error("EXCEPTION URL DELETE:" + url + ", exception message: " + e.getMessage());
            throw e;
        }

        deleteMethod.releaseConnection();
        logger.trace("Response: " + responseString);

        return responseString;
    }

    /**
     * Logs the url and the params passed if debug is enabled
     *
     * @param url    absolute URL passed for a logging reference
     * @param params params passed for a logging reference
     */
    private static void logParams(String url, List<NameValuePair> params) {
        if (logger.isDebugEnabled()) {
            //logger.debug("URL: " + url);
            if (params != null) {
                for (NameValuePair oneParam : params) {
                    //logger.debug("Param: " + oneParam.getName() + ", Value: " + oneParam.getValue());
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

    public static class IdleConnectionMonitorThread extends Thread {
        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread
                (PoolingHttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(1000);
                        connMgr.closeExpiredConnections();
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                shutdown();
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Closes any thread that is still running
     */
    private static void closeWaitingThreads() {
        logger.debug("Closing idle http threads to let the job exits");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread t : threadSet) {
            if (t.getState().equals(Thread.State.TIMED_WAITING)) {
                t.interrupt();
                logger.debug("Thread : " + t.getName() + ", Closed");
            }
        }
    }

}