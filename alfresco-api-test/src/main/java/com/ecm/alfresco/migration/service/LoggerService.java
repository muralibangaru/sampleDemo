package com.ecm.alfresco.migration.service;

import com.ecm.alfresco.migration.bean.counter.Counter;
import com.ecm.alfresco.migration.job.config.MigrationProperties;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.util.Map;

public class LoggerService {
    private static final Logger logger = Logger.getLogger(LoggerService.class);

    /**
     * Prints final results
     *
     * @param status  migration status
     * @param batchId batch ID
     * @param time    completion time
     * @param counter counter
     */
    public void printResults(String status, String batchId, String time, Counter counter) {

        if (batchId != null) {
            logger.info("");
            logger.info("################################################################");
            logger.info(" MIGRATION REPORT SUMMARY batchId " + batchId + " STATUS: " + status);
            logger.info("################################################################");
            logger.info("Report batchId -" + batchId + "- Destination: " + MigrationProperties.get(MigrationProperties.PROP_TARGET_STRUCTURE_ROOT_PATH));
            logger.info("Report batchId -" + batchId + "- Total Time: " + time);
            logger.info("");
            logger.info("Report batchId -" + batchId + "- Documents Extracted: " + counter.getCounterExtractedDocs());
            logger.info("Report batchId -" + batchId + "- Documents Processed: " + counter.getCounterProcessed());
            logger.info("Report batchId -" + batchId + "- Documents Migrated Success: " + counter.getCounterMigrated());
            logger.info("Report batchId -" + batchId + "- Documents Migrated New: " + counter.getCounterNew());
            logger.info("Report batchId -" + batchId + "- Documents Migrated Skipped: " + counter.getCounterSkipped());
            logger.info("Report batchId -" + batchId + "- Documents Migrated Replaced: " + counter.getCounterReplaced());
            logger.info("Report batchId -" + batchId + "- Documents Migrated Failed: " + counter.getCounterFailed());
            logger.info("");
        }
    }

    /**
     * Initializes log4J configuration for a particular batch
     */
    public void initBatchLog4JConfiguration(String batchFolderPath) {
        String batchLevel = MigrationProperties.get(MigrationProperties.PROP_BATCH_LOG_LEVEL);
        Level level = Level.INFO;

        if (batchLevel != null) {
            switch (batchLevel.toUpperCase()) {
                case ("OFF"):
                    level = Level.OFF;
                    break;
                case ("FATAL"):
                    level = Level.FATAL;
                    break;
                case ("ERROR"):
                    level = Level.ERROR;
                    break;
                case ("WARN"):
                    level = Level.WARN;
                    break;
                case ("INFO"):
                    level = Level.INFO;
                    break;
                case ("DEBUG"):
                    level = Level.DEBUG;
                    break;
                case ("TRACE"):
                    level = Level.TRACE;
                    break;
                case ("ALL"):
                    level = Level.ALL;
                    break;
            }
        }

        String batchLogPath = batchFolderPath + "/" + "batch.log";
        logger.debug("Batch log level: " + batchLevel + ", Batch log path: " + batchLogPath);
        FileAppender fileAppender = new FileAppender();
        fileAppender.setName("FileLogger");
        fileAppender.setFile(batchLogPath);
        fileAppender.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n"));
        fileAppender.setThreshold(level);
        fileAppender.setAppend(true);
        fileAppender.activateOptions();
        Logger.getRootLogger().addAppender(fileAppender);
    }
}
