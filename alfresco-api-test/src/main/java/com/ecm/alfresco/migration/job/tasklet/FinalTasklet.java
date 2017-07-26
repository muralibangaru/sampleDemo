package com.ecm.alfresco.migration.job.tasklet;

import com.ecm.alfresco.migration.job.param.JobParameters;
import com.ecm.alfresco.migration.service.LoggerService;
import com.ecm.alfresco.migration.util.ExcelUtil;
import com.ecm.alfresco.migration.util.HttpPoolHelper;
import com.ecm.alfresco.migration.util.TimeUtil;

import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FinalTasklet implements Tasklet {

    @Autowired
    private LoggerService loggerService;
    @Autowired
    private JobParameters jobParameters;

    private static final Logger logger = Logger.getLogger(FinalTasklet.class);

    /**
     * Executes the tasklet
     * @param contribution
     * @param chunkContext
     * @return the status
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.debug("Starting final tasklet");
        String status = getStatus();
        loggerService.printResults(status, jobParameters.getBatchId(), jobParameters.getStopWatchTotal().toString(), jobParameters.getCounter());

        logger.debug("Closing HTTP pool");
        HttpPoolHelper.closeAllConnections();

        return RepeatStatus.FINISHED;
    }

    /**
     * Gets job status
     * @return
     */
    private String getStatus() {
        if (jobParameters.getCounter().getCounterFailed() == 0)
            return "SUCCESS";
        else
            return "ERROR";
    }

}
