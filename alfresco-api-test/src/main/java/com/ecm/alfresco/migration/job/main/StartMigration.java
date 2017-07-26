package com.ecm.alfresco.migration.job.main;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.ecm.alfresco.migration.job.config.MigrationJobConfiguration;
import com.ecm.alfresco.migration.job.config.MigrationProperties;

@SpringBootApplication
public class StartMigration {
    private static final Logger logger = Logger.getLogger(StartMigration.class);

    public static void main(String [] args) throws Exception {
        logger.info("Starting migration");
        setMigrationProperties(args);
        SpringApplication.run(MigrationJobConfiguration.class, args);
    }

    /**
     * Extracts the profile from the arguments
     * @param args
     * @throws Exception
     */
    private static void setMigrationProperties(String[] args) throws Exception {
        String profile = "DEFAULT";

        if(args.length > 0) {
            profile = args[0];
            if (profile == null || profile.isEmpty()) {
                profile = "DEFAULT";
            }
        }

        logger.info("Migration Profile: " + profile);
        MigrationProperties.loadProperties(profile);
    }

}
