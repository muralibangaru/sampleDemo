package com.ecm.alfresco.migration.util;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;

import com.ecm.alfresco.migration.job.config.MigrationProperties;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Miguel Sanchez
 * @version 1.0
 *          2016-04-04
 */
public class TimeUtil {
    private static final Logger logger = Logger.getLogger(TimeUtil.class);

    /**
     * Calculates the estimated time to complete the process
     *
     * @param i         current index
     * @param stopWatch stopwatch
     * @param listSize  list size
     * @return the estimated time
     */
    public static String getEstimatedTime(int i, StopWatch stopWatch, long listSize) {
        if (i > 0) {
            long estimatedTimeMillis = (stopWatch.getTime() / i) * (listSize - i);
            long second = (estimatedTimeMillis / 1000) % 60;
            long minute = (estimatedTimeMillis / (1000 * 60)) % 60;
            long hour = (estimatedTimeMillis / (1000 * 60 * 60)) % 24;

            return String.format("%02dh %02dm %02ds", hour, minute, second);

        } else
            return "";
    }

    /**
     * Calculates the rate
     *
     * @param i         current index
     * @param stopWatch stopwatch
     * @return the current rate
     */
    public static String getRate(int i, StopWatch stopWatch) {
        if (i > 0) {
            return new DecimalFormat("##.##").format(((float) (i * 1000) / stopWatch.getTime())) + " docs/s";

        } else
            return "*";

    }

    /**
     * Creates a stopwatch and starts it
     *
     * @return stopwatch
     */
    public static StopWatch startStopWatch() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        return stopWatch;
    }

    /**
     * print stopwatch split
     *
     * @param stopWatch stopwatch to split
     * @return stopwatchsplit
     */
    public static String printSplit(StopWatch stopWatch) {
        if (Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_NUM_THREADS)) == 1) {
            if (stopWatch != null) {
                return stopWatch.toString();

            } else
                return "0";
        } else
            return "0";
    }

    /**
     * Gets current time in String format
     *
     * @return current datetime
     */
    public static String getNowTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    /**
     * Gets today date in String format
     *
     * @return date string
     */
    public static String getTodayDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        return simpleDateFormat.format(new Date());
    }

    /**
     * Resumes a stopwatch
     * @param type
     * @param stopWatch
     */
    public static void resume(String type, StopWatch stopWatch) {
        if (Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_NUM_THREADS)) == 1) {
            if (stopWatch.isStopped()) {
                logger.trace(type + ", StopWatch is stopped");
                stopWatch.reset();
                stopWatch.start();

            } else if (stopWatch.isSuspended()) {
                logger.trace(type + ", StopWatch is suspended");
                stopWatch.resume();

            } else {
                logger.trace(type + ", Stopwatch was running but it should have been stopped");
            }
        }
    }

    /**
     * Suspends a stopwatch
     * @param type
     * @param stopWatch
     */
    public static void suspend(String type, StopWatch stopWatch) {
        if (Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_NUM_THREADS)) == 1) {
            if (stopWatch.isStopped()) {
                logger.trace(type + ", StopWatch is stopped");

            } else if (stopWatch.isSuspended()) {
                logger.trace(type + ", StopWatch is suspended");

            } else if (stopWatch.isStarted()) {
                stopWatch.suspend();
                logger.trace(type + ", Stopwatch has been suspended");

            } else {
                stopWatch.suspend();
                logger.trace(type + ", Stopwatch has been suspended");
            }
        }
    }
}
