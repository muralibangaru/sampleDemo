package com.ecm.alfresco.migration.component;

import org.apache.commons.dbcp2.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ecm.alfresco.migration.job.config.MigrationProperties;

import javax.annotation.PostConstruct;
import java.sql.SQLException;

public class DataSourceComponent {
    private static final Logger logger = Logger.getLogger(DataSourceComponent.class);
    private static final String DEFAULT_DB_POOL = "6";
    private BasicDataSource basicDataSource;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private static String databaseDriver;
    private static String databaseURL;
    private static String databaseUsername;
    private static String databasePassword;
    
   

    @PostConstruct
    private void init() {
                
        String databasePool = MigrationProperties.get(MigrationProperties.PROP_NUM_THREADS);

        if (!StringUtils.isNumeric(databasePool)) {
            logger.error("Propety " + MigrationProperties.PROP_NUM_THREADS + " is not set in 'migration.properties'. Setting DB pool to " + DEFAULT_DB_POOL);
            databasePool = DEFAULT_DB_POOL;
        }

        logger.debug("data source parameters: " + databasePool + "," + databaseDriver + "," + databaseURL);

        basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(databaseDriver);
        basicDataSource.setUrl(databaseURL);
        basicDataSource.setUsername(databaseUsername);
        basicDataSource.setPassword(databasePassword);
        basicDataSource.setInitialSize(Integer.parseInt(databasePool));
        jdbcTemplate = new NamedParameterJdbcTemplate(basicDataSource);
                
    }

    public void close(){
        try {
            basicDataSource.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
    
}