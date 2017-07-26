package com.ecm.alfresco.migration.job.reader;

import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.service.DocumentService;
import com.ecm.alfresco.migration.util.CmisHelper;
import com.ecm.alfresco.migration.util.TimeUtil;

import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

public class DocumentCMISReader implements ItemReader<DocumentItem> {
    @Autowired
    private JobParameters jobParameters;
    @Autowired
    private DocumentService documentService;

    private static final Logger logger = Logger.getLogger(DocumentCMISReader.class);

    /**
     * Reads documents and passes them to the writer
     * @return
     */
    @Override
    public DocumentItem read() {
        try {
            TimeUtil.resume("NODEREF", jobParameters.getStopWatchGetNodeRef());
            String documentId = getNextDocumentId();

            if (documentId != null) {
                logger.debug("Document read: " + documentId);
                TimeUtil.suspend("NODEREF", jobParameters.getStopWatchGetNodeRef());
                return new DocumentItem(documentId);
            }

        }catch (Exception e) {
            TimeUtil.suspend("NODEREF", jobParameters.getStopWatchGetNodeRef());
            throw e;
        }

        return null;
    }

    /**
     * Gets next document id from the current page
     * @return
     */
    public String getNextDocumentId() {
        try {
            String oneDocumentId = jobParameters.getNextDocumentId();
            if (oneDocumentId == null) {
                ItemIterable<QueryResult> onePage = getNextPageByQuery(jobParameters);
                if (onePage == null || !onePage.iterator().hasNext()) {
                    logger.debug("No more documents left to process");
                    return null;

                } else {
                    jobParameters.addAll(onePage);
                    return getNextDocumentId();
                }
            } else {
                logger.info(Thread.currentThread().getName() + " - " + jobParameters.getCounter().increaseCounterExtractedDocs() + " - EXTRACTED: " + oneDocumentId);
                return oneDocumentId;
            }
        } catch (CmisInvalidArgumentException e) {
            logger.error("Check value of property \"source.location\", it can't be empty");
            throw e;
        }
    }

    /**
     * Gets next page based on a CMIS query
     * @param jobParameters
     * @return
     */
    public synchronized ItemIterable<QueryResult> getNextPageByQuery(JobParameters jobParameters) {
        logger.debug("Extracting page " + jobParameters.getCounter().getPageIndex());
        return CmisHelper.getDocuments(jobParameters.getSessionSource(), jobParameters.getQuery(), jobParameters.getCounter().increasePageIndex(), jobParameters.getPageSize());
    }
}
