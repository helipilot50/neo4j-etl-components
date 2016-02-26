package org.neo4j.integration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.neo4j.integration.mysql.MySqlClient;
import org.neo4j.integration.neo4j.Neo4j;
import org.neo4j.integration.neo4j.Neo4jVersion;
import org.neo4j.integration.provisioning.Neo4jFixture;
import org.neo4j.integration.provisioning.Server;
import org.neo4j.integration.provisioning.ServerFixture;
import org.neo4j.integration.provisioning.scripts.MySqlScripts;
import org.neo4j.integration.sql.DatabaseType;
import org.neo4j.integration.util.ResourceRule;
import org.neo4j.integration.util.Strings;
import org.neo4j.integration.util.TemporaryDirectory;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class ExportFromMySqlIntegrationTest
{
    private static final Neo4jVersion NEO4J_VERSION = Neo4jVersion.v3_0_0_M04;

    @ClassRule
    public static final ResourceRule<Path> tempDirectory = new ResourceRule<>( TemporaryDirectory.temporaryDirectory
            () );

    @ClassRule
    public static final ResourceRule<Server> mySqlServer = new ResourceRule<>(
            ServerFixture.server(
                    "mysql-integration-test",
                    DatabaseType.MySQL.defaultPort(),
                    MySqlScripts.startupScript(),
                    tempDirectory.get() ) );

    @ClassRule
    public static final ResourceRule<Neo4j> neo4j = new ResourceRule<>(
            Neo4jFixture.neo4j( NEO4J_VERSION, tempDirectory.get() ) );

    @BeforeClass
    public static void setUp() throws Exception
    {
        populateMySqlDatabase();
    }

    @Test
    public void shouldExportFromMySqlAndImportIntoGraph() throws Exception
    {
        // when
        exportFromMySqlToNeo4j( "Person", "Address" );

        // then
        neo4j.get().start();


        String response = neo4j.get().executeHttp( "http://localhost:7474/db/data/transaction/commit",
                requestEntityUsingJackson() );
        List<String> usernames = JsonPath.read( response, "$.results[*].data[*].row[*].username" );
        List<String> postcodes = JsonPath.read( response, "$.results[*].data[*].row[*].postcode" );
        assertThat( usernames, hasItems( "user-1", "user-2", "user-3", "user-4", "user-5", "user-6", "user-7",
                "user-8", "user-9" ) );
        assertThat( postcodes, hasItems( "AB12 1XY", "XY98 9BA", "ZZ1 0MN" ) );
        neo4j.get().stop();
    }

    @Test
    public void shouldExportFromMySqlAndImportIntoGraphForNumericAndStringTables() throws Exception
    {
        // when
        exportFromMySqlToNeo4j( "String_Table", "Numeric_Table" );

        // then
        neo4j.get().start();

        String expectedResults = Strings.lineSeparated(
                "+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+",
                "| n                                                                                                 " +
                        "                                                                                            " +
                        "                                                                                            " +
                        "                                                |",
                "+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+",
                "| Node[0]{char_field:\"char-field\",text_field:\"text_field\",blob_field:\"blob_field\"," +
                        "tinytext_field:\"tinytext_field\",tinyblob_field:\"tinyblob_field\"," +
                        "mediumtext_field:\"mediumtext_field\",mediumblob_field:\"mediumblob_field\"," +
                        "longtext_field:\"longtext_field\",longblob_field:\"longblob_field\",enum_field:\"val-1\"," +
                        "varchar_field:\"varchar-field\"} |",
                "| Node[1]{tinyint_field:1,smallint_field:123,mediumint_field:123,bigint_field:123,float_field:123.2," +
                        "double_field:1.232343445E7,decimal_field:18.0}                                              " +
                        "                                                                                            " +
                        "                                                |",
                "+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+" );

        String response = neo4j.get().executeHttp( "http://localhost:7474/db/data/transaction/commit",
                requestEntityUsingJackson() );
        List<Map<String, String>> stringFields = JsonPath.read( response, "$.results[*].data[0].row[0]" );
        List<Map<String, Object>> numericFields = JsonPath.read( response, "$.results[*].data[1].row[0]" );
        assertThat( stringFields.get( 0 ).values(), hasItems( "val-1", "mediumtext_field", "longblob_field",
                "blob_field",
                "tinytext_field", "mediumblob_field", "char-field", "text_field", "varchar-field",
                "tinyblob_field", "longtext_field" ) );
        assertThat( numericFields.get( 0 ).values(), hasItems( 123, 123, 123.2, 123, 18.0, 1.232343445E7, 1 ) );
        neo4j.get().stop();
    }

    private void exportFromMySqlToNeo4j( String parent, String child )
    {
        NeoIntegrationCli.executeMainReturnSysOut(
                new String[]{"mysql-export",
                        "--host", mySqlServer.get().ipAddress(),
                        "--user", MySqlClient.Parameters.DBUser.value(),
                        "--password", MySqlClient.Parameters.DBPassword.value(),
                        "--database", MySqlClient.Parameters.Database.value(),
                        "--import-tool", neo4j.get().binDirectory().toString(),
                        "--csv-directory", tempDirectory.get().toString(),
                        "--destination", neo4j.get().databasesDirectory().resolve( "graph.db" ).toString(),
                        "--parent", parent,
                        "--child", child} );
    }

    private static void populateMySqlDatabase() throws Exception
    {
        MySqlClient client = new MySqlClient( mySqlServer.get().ipAddress() );
        client.execute( MySqlScripts.setupDatabaseScript().value() );
    }

    public String requestEntityUsingJackson() throws JsonProcessingException
    {
        Statements statements = new Statements();
        statements.add( new Statement( "MATCH (n) RETURN n;" ) );
        return new ObjectMapper().writeValueAsString( statements );
    }

    public class Statements
    {
        @JsonProperty("statements")
        private List<Statement> statements = new ArrayList<>();

        private Statements add( Statement statement )
        {
            statements.add( statement );
            return this;
        }
    }

    public class Statement
    {
        @JsonProperty("statement")
        public String value;

        public Statement( String value )
        {
            this.value = value;
        }
    }
}
