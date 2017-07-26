package com.ecm.alfresco.migration.job.reader;

import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.util.TimeUtil;

import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DocumentDatasourceReader implements ItemReader<DocumentItem> {
    @Autowired
    private JobParameters jobParameters;
   
    private static final Logger logger = Logger.getLogger(DocumentDatasourceReader.class);

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

        } catch (Exception e) {
            TimeUtil.suspend("NODEREF", jobParameters.getStopWatchGetNodeRef());
            throw e;
        }

        return null;
    }

    /**
     * Gets the next document id from the current page extracted from the database
     * @return
     */
    public String getNextDocumentId() {
        String oneDocumentId = jobParameters.getNextDocumentId();
        if (oneDocumentId == null) {
            List<String> onePage = null;
            if (onePage == null || onePage.size() == 0) {
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
    }

   
    /*private List<String> getFailedNextPage() {
        logger.debug("Extracting failed documents page " + jobParameters.getCounter().getPageIndex());
        return datasourceService.getFailedDocuments(jobParameters.getReportDBTable(), jobParameters.getCounter().increasePageIndex(), jobParameters.getPageSize());
    }*/
}
