package com.ecm.alfresco.migration.job.config;

import com.ecm.alfresco.migration.bean.document.DocumentAssociation;
import com.ecm.alfresco.migration.bean.document.DocumentItem;
import com.ecm.alfresco.migration.component.DataSourceComponent;
import com.ecm.alfresco.migration.job.param.JobParameters;

import com.ecm.alfresco.migration.job.reader.DocumentCMISReader;
import com.ecm.alfresco.migration.job.reader.DocumentDatasourceReader;
import com.ecm.alfresco.migration.job.reader.FolderReader;
import com.ecm.alfresco.migration.job.tasklet.FinalTasklet;
import com.ecm.alfresco.migration.job.tasklet.InitTasklet;
import com.ecm.alfresco.migration.job.writer.AssociationWriter;
import com.ecm.alfresco.migration.job.writer.DocumentWriter;
import com.ecm.alfresco.migration.service.*;

import org.apache.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class MigrationJobConfiguration {
    private static final Logger logger = Logger.getLogger(MigrationJobConfiguration.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    /**
     * Creates migration job
     * @return
     * @throws Exception
     */
    @Bean
    public Job job() throws Exception {
        return jobBuilderFactory.get("migrationJob").
                start(initStep()).
                next(migrationStep()).
                build();
    }

    /**
     * Creates the migration step
     * @return
     * @throws Exception
     */
    @Bean
    public Step migrationStep() throws Exception {
        String threads = MigrationProperties.get(MigrationProperties.PROP_NUM_THREADS);
        String profile = MigrationProperties.get(MigrationProperties.PROP_MIGRATION_PROFILE);

        if (threads != null) {
            int intThreads = Integer.parseInt(threads);

                return getSimpleStepBuilder()
                        .taskExecutor(new SimpleAsyncTaskExecutor())
                        .throttleLimit(intThreads)
                        .build();
        } else {
            
                return getSimpleStepBuilder().build();
        }
    }

    /**
     * Creates the step builder for documentItem
     * @return
     * @throws Exception
     */
    private SimpleStepBuilder<DocumentItem, DocumentItem> getSimpleStepBuilder() throws Exception {
        try {
            int batchSize = Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_SOURCE_BATCH_SIZE));
            return stepBuilderFactory.get("migrate")
                    .<DocumentItem, DocumentItem>chunk(batchSize)
                    .reader(reader())
                    .writer(writer());

        } catch (Exception e) {
            logger.error(MigrationProperties.PROP_SOURCE_BATCH_SIZE + " value is not a integer: " + MigrationProperties.get(MigrationProperties.PROP_SOURCE_BATCH_SIZE));
            throw e;
        }
    }

    /**
     * Creates the step builder for documentAssociation
     * @return
     * @throws Exception
     
    private SimpleStepBuilder<DocumentAssociation, DocumentAssociation> getAssociationStepBuilder() throws Exception {
        try {
            int batchSize = Integer.parseInt(MigrationProperties.get(MigrationProperties.PROP_SOURCE_BATCH_SIZE));
            return stepBuilderFactory.get("migrate")
                    .<DocumentAssociation, DocumentAssociation>chunk(batchSize)
                    .reader(associationReader())
                    .writer(associationWriter());

        } catch (Exception e) {
            logger.error(MigrationProperties.PROP_SOURCE_BATCH_SIZE + " value is not a integer: " + MigrationProperties.get(MigrationProperties.PROP_SOURCE_BATCH_SIZE));
            throw e;
        }
    }*/

    /**
     * Creates the reader based on the profile
     * @return
     * @throws Exception
     */
    @Bean
    public ItemReader<DocumentItem> reader() throws Exception {
        String profile = MigrationProperties.get(MigrationProperties.PROP_MIGRATION_PROFILE);

        if("FAILED".equalsIgnoreCase(profile))
            return new DocumentDatasourceReader();

        else if("RUN".equalsIgnoreCase(profile) || "RERUN".equalsIgnoreCase(profile))
            return new FolderReader();

        else if ("DELTA".equalsIgnoreCase(profile) || "QUERY".equalsIgnoreCase(profile))
            return new DocumentCMISReader();

        else if ("FOLDER".equalsIgnoreCase(profile))
            return new FolderReader();

        else
            return new FolderReader();
    }

    /**
     * Creates the final tasklet
     * @return
     
    @Bean
    public Tasklet finalTasklet() {
        return new FinalTasklet();
    }*/

    /**
     * Creates the final step
     * @return
     
    @Bean
    public Step finalStep() {
        return stepBuilderFactory.get("final").tasklet(finalTasklet()).build();
    }*/

    /**
     * Creates the initial tasklet
     * @return
     */
    @Bean
    public Tasklet initTasklet() {
        return new InitTasklet();
    }

    /**
     * Creates the initial step
      * @return
     */
    @Bean
    public Step initStep() {
        return stepBuilderFactory.get("init").tasklet(initTasklet()).build()    ;
    }

    /**
     * Creates the job parameters
     * @return
     */
    @Bean
    public JobParameters jobParameters() {
        return new JobParameters();
    }

    /**
     * Creates the document service
     * @return
     */
    @Bean
    public DocumentService documentService() {
        return new DocumentService();
    }

    /**
     * Creates the loggerService
     * @return
     */
    @Bean
    public LoggerService loggerService() {
        return new LoggerService();
    }

    /**
     * Creates the validationService
     * @return
     */
    @Bean
    public ValidationService validationService() {
        return new ValidationService();
    }

    /**
     * Creates the dataSourceComponent
     * @return
     */
    @Bean
    public DataSourceComponent dataSourceComponent() {
        return new DataSourceComponent();
    }


    /**
     * Creates the alfrescoAPIService
     * @return
     */
    @Bean
    public AlfrescoAPIService alfrescoAPIService() {
        return new AlfrescoAPIService();
    }

    /**
     * Creates the associationReader
     * @return
     
    @Bean
    public ItemReader<DocumentAssociation> associationReader() throws Exception {
        return new AssociationDatasourceReader();
    }*/

    /**
     * Creates the writer
     * @return
     */
    @Bean
    public ItemWriter<DocumentItem> writer() {
            return new DocumentWriter();
    }

    /**
     * Creates the associationWriter
     * @return
     */
    @Bean
    public ItemWriter<DocumentAssociation> associationWriter() {
        return new AssociationWriter();
    }

}
