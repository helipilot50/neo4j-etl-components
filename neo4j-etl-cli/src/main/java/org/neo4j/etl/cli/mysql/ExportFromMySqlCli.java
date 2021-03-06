package org.neo4j.etl.cli.mysql;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.OptionType;
import com.github.rvesse.airline.annotations.restrictions.Required;
import org.apache.commons.lang3.StringUtils;

import org.neo4j.etl.commands.mysql.ExportFromMySql;
import org.neo4j.etl.commands.mysql.GenerateMetadataMapping;
import org.neo4j.etl.environment.CsvDirectorySupplier;
import org.neo4j.etl.environment.DestinationDirectorySupplier;
import org.neo4j.etl.environment.Environment;
import org.neo4j.etl.environment.ImportToolDirectorySupplier;
import org.neo4j.etl.neo4j.importcsv.config.formatting.Formatting;
import org.neo4j.etl.neo4j.importcsv.config.formatting.ImportToolOptions;
import org.neo4j.etl.sql.ConnectionConfig;
import org.neo4j.etl.sql.DatabaseType;
import org.neo4j.etl.sql.exportcsv.io.TinyIntResolver;
import org.neo4j.etl.sql.exportcsv.mapping.FilterOptions;
import org.neo4j.etl.sql.exportcsv.mapping.MetadataMappings;
import org.neo4j.etl.sql.exportcsv.mapping.TinyIntAs;
import org.neo4j.etl.sql.exportcsv.mysql.MySqlExportSqlSupplier;
import org.neo4j.etl.util.CliRunner;

@Command(name = "export", description = "Export from MySQL.")
public class ExportFromMySqlCli implements Runnable
{
    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"-h", "--host"},
            description = "Host to use for connection to MySQL.",
            title = "hostname")
    private String host = "localhost";

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"-p", "--port"},
            description = "Port number to use for connection to MySQL.",
            title = "port #")
    private int port = 3306;

    @Required
    @Option(type = OptionType.COMMAND,
            name = {"-u", "--user"},
            description = "User for login to MySQL.",
            title = "username")
    private String user;

    @Option(type = OptionType.COMMAND,
            name = {"--password"},
            description = "Password for login to MySQL.",
            title = "password")
    private String password;

    @Required
    @Option(type = OptionType.COMMAND,
            name = {"-d", "--database"},
            description = "MySQL database.",
            title = "name")
    private String database;

    @Required
    @Option(type = OptionType.COMMAND,
            name = {"--import-tool"},
            description = "Path to directory containing Neo4j import tool.",
            title = "directory")
    private String importToolDirectory;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--options-file"},
            description = "Path to file containing Neo4j import tool options.",
            title = "file")
    private String importToolOptionsFile = "";

    @Required
    @Option(type = OptionType.COMMAND,
            name = {"--csv-directory"},
            description = "Path to directory for intermediate CSV files.",
            title = "directory")
    private String csvRootDirectory;

    @Required
    @Option(type = OptionType.COMMAND,
            name = {"--destination"},
            description = "Path to destination store directory.",
            title = "directory")
    private String destinationDirectory;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--force"},
            description = "Force delete destination store directory if it already exists.")
    private boolean force = false;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--delimiter"},
            description = "Delimiter to separate fields in CSV.",
            title = "character")
    private String delimiter;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--quote"},
            description = "Character to treat as quotation character for values in CSV data.",
            title = "character")
    private String quote;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--debug"},
            description = "Print detailed diagnostic output.")
    private boolean debug = false;

    @Option(type = OptionType.COMMAND,
            name = {"--mapping-file"},
            description = "Path to an existing metadata mapping file. " +
                    "The name 'stdin' will cause the CSV resources definitions to be read from standard input.",
            title = "file|stdin")
    private String mappingFile;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--relationship-name", "--rel-name"},
            description = "Specifies whether to get the name for relationships from table names or column names",
            title = "table(default)|column")
    private String relationshipNameFrom = "table";

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--tiny-int", "--tiny"},
            description = "Specifies whether to get the convert TinyInt to byte or boolean",
            title = "byte(default)|boolean")
    private String tinyIntAs = "byte";

    @SuppressWarnings("FieldCanBeLocal")
    @Option(type = OptionType.COMMAND,
            name = {"--exclusion-mode", "--exc"},
            description = "Specifies how to handle table exclusion. Options are mutually exclusive." +
                    "exclude: Excludes specified tables from the process. All other tables will be included." +
                    "include: Includes specified tables only. All other tables will be excluded." +
                    "none: All tables are included in the process.",
            title = "exclude|include|none(default)")
    private String exclusionMode = "none";

    @SuppressWarnings("FieldCanBeLocal")
    @Arguments(description = "Tables to be excluded/included",
            title = "table1 table2 ...")
    private List<String> tables = new ArrayList<String>();

    @Override
    public void run()
    {
        try
        {
            ConnectionConfig connectionConfig = ConnectionConfig.forDatabase( DatabaseType.MySQL )
                    .host( host )
                    .port( port )
                    .database( database )
                    .username( user )
                    .password( password )
                    .build();

            Environment environment = new Environment(
                    new ImportToolDirectorySupplier( Paths.get( importToolDirectory ) ).supply(),
                    new DestinationDirectorySupplier( Paths.get( destinationDirectory ), force ).supply(),
                    new CsvDirectorySupplier( Paths.get( csvRootDirectory ) ).supply(),
                    ImportToolOptions.initialiseFromFile( Paths.get( importToolOptionsFile ) ) );

            ImportToolOptions importToolOptions = environment.importToolOptions();

            Formatting formatting = Formatting.builder()
                    .delimiter( importToolOptions.getDelimiter( delimiter ) )
                    .quote( importToolOptions.getQuoteCharacter( quote ) )
                    .build();

            TinyIntResolver tinyIntResolver = new TinyIntResolver( TinyIntAs.parse( tinyIntAs ) );
            MetadataMappings metadataMappings = createMetadataMappings( connectionConfig, formatting, tinyIntResolver );

            new ExportFromMySql(
                    new ExportMySqlEventHandler(),
                    metadataMappings,
                    connectionConfig,
                    formatting,
                    environment, tinyIntResolver ).call();
        }
        catch ( Exception e )
        {
            CliRunner.handleException( e, debug );
        }
    }

    private MetadataMappings createMetadataMappings( ConnectionConfig connectionConfig,
                                                     Formatting formatting,
                                                     TinyIntResolver tinyIntResolver ) throws Exception
    {
        Callable<MetadataMappings> generateMetadataMappings;

        if ( StringUtils.isNotEmpty( mappingFile ) )
        {
            generateMetadataMappings = GenerateMetadataMappingCli.metadataMappingsFromFile( mappingFile );
        }
        else
        {
            final FilterOptions filterOptions = new FilterOptions( tinyIntAs, relationshipNameFrom, exclusionMode,
                    tables, false );
            generateMetadataMappings = new GenerateMetadataMapping(
                    new GenerateMetadataMappingEventHandler(),
                    emptyOutputStream(),
                    connectionConfig,
                    formatting,
                    new MySqlExportSqlSupplier(),
                    filterOptions,
                    tinyIntResolver );
        }

        return generateMetadataMappings.call();
    }

    private OutputStream emptyOutputStream()
    {
        return new OutputStream()
        {
            @Override
            public void write( int b ) throws IOException
            {
                // Do nothing
            }
        };
    }

}
