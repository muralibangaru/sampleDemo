package com.ecm.alfresco.migration.job.writer;

import com.ecm.alfresco.migration.bean.document.DocumentAssociation;
import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.util.TimeUtil;

import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssociationWriter implements ItemWriter<DocumentAssociation> {
    private static final String SUCCESS = "SUCCESS";
    private static final String SKIPPED = "SKIPPED";
    private static final String FAILED = "FAILED";
    private static final String EXCLUDED = "EXCLUDED";

    
    @Autowired
    private JobParameters jobParameters;

    private static final Logger logger = Logger.getLogger(AssociationWriter.class);

    /**
     * Creates associations in the target repository
     * @param documentAssociationList
     * @throws Exception
     */
    @Override
    public void write(List<? extends DocumentAssociation> documentAssociationList) throws Exception {
        for (DocumentAssociation association : documentAssociationList) {
            StopWatch stopwatch = TimeUtil.startStopWatch();

            if (association != null) {
                try {
                    createAssociation(association, stopwatch);
                    processSuccess(association, stopwatch);

                } catch (Exception e) {
                    processException(association, stopwatch, e);

                    if (e instanceof NullPointerException) {
                        e.printStackTrace();

                    } else if (logger.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Creates an association in the target repository
     * @param association
     * @param stopwatch
     * @throws Exception
     */
    private void createAssociation(DocumentAssociation association, StopWatch stopwatch) throws Exception {
        checkSkipAssociation(association, stopwatch);// check if the association has to be skipped

        if (!SKIPPED.equals(association.getStatus()) && !EXCLUDED.equals(association.getStatus())) {

            try {
                Map<String, String> associationProperties = new HashMap<>();
                associationProperties.put("cmis:sourceId", association.getTarRepoSourceId());
                associationProperties.put("cmis:targetId", association.getTarRepoTargetId());
                associationProperties.put("cmis:objectTypeId", association.getType());
                jobParameters.getSessionTarget().createRelationship(associationProperties);
                association.setStatus(SUCCESS);

            } catch (CmisRuntimeException e) {
                // the association already exists
                logger.debug("The association already exists, Target Repo Source Node ID: " + association.getTarRepoSourceId() + ", Target Repo Target ID: " + association.getTarRepoTargetId());
                association.setStatus(SKIPPED);
            }
        }
    }

    /**
     * Verifies if the association can be skipped
     * @param association
     * @param stopWatch
     * @throws Exception
     */
    private void checkSkipAssociation(DocumentAssociation association, StopWatch stopWatch) throws Exception {
        String status = association.getStatus();

        if (association.getType().equals("R:cm:workingcopylink")
        		|| association.getType().equals("R:cm:original")) { // do not process working copies associations)
            association.setStatus(EXCLUDED);

        } else if (status != null && (status.equals(SUCCESS) || status.equals(SKIPPED))) {
            association.setStatus(SKIPPED);
        }
    }

    /**
     * Process association creation success
     * @param association
     * @param stopWatch
     * @throws Exception
     */
    private void processSuccess(DocumentAssociation association, StopWatch stopWatch) throws Exception {
        jobParameters.getCounter().increaseCounterProcessed();
        jobParameters.getCounter().increaseCounterMigrated();
        jobParameters.getCounter().increaseCounterNew();
    }

    /**
     * Proccess association creation expception
     * @param association
     * @param stopWatch
     * @param e
     * @throws Exception
     */
    private void processException(DocumentAssociation association, StopWatch stopWatch, Exception e) throws Exception {
        logger.error("EXCEPTION Source Repo Source Node ID: " + association.getSrcRepoSourceId() + ", Source Repo Target ID: " + association.getSrcRepoTargetId());
        association.setStatus(FAILED);
        association.setMessage(e.getMessage());
        jobParameters.getCounter().increaseCounterProcessed();
        jobParameters.getCounter().increaseCounterFailed();
        
        if (logger.isDebugEnabled()) {
            e.printStackTrace();
        }
    }
}
