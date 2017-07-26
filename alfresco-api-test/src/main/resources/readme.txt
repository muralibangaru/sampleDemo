###############################################################################################

         ALFRESCO MIGRATION TOOL (a CMIS/API REST migration solution using Spring boot & batch)

##############################################################################################

- GENERATE THE JAR alfresco-migration-tool.jar
    To generate the jar file execute "mvn clean install" from the command line
    Maven will create an output folder "alfresco-migration-tool" that will contain
    . jar "alfresco-migration-tool.jar"
    . folder "config" that will contain the properties files "migration.properties"
    . folder "lib" that will contain all the dependencies

- REQUIREMENTS TO RUN THE TOOL
    Copy the folder "alfresco-migration-tool" to the server where the tool will run.
    The folder should contain
        . jar file alfresco-migration-tool.jar
        . folder "config"
        . folder "lib"

    Configure properties files
        . "migration.properties" contains the information about folder locations, metadata file, server URL and credentials

- RUN THE TOOL
    Execute from the command line "java -jar alfresco-migration-tool.jar"
    This can be run from eclipse i.e. "Run as a Java Application" or "Run as Spring bootApp"

####################################################
	Test Data preparation & testing the tool
####################################################	

- Create Test01 & Test02 folder under company home/repository in Alfresco.
- Upload documents in Test01 folder.
- Execute the tool.
- This will create exactly a replica of Test01 & its documents Under Test02