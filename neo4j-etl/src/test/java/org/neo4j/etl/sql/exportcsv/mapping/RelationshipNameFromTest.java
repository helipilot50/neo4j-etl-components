package org.neo4j.etl.sql.exportcsv.mapping;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RelationshipNameFromTest
{
    @Test
    public void parseRelationshipNameOrReturnTableNameByDefault() throws Exception
    {
        assertThat( RelationshipNameFrom.TABLE_NAME, is( RelationshipNameFrom.parse( "table" ) ) );
        assertThat( RelationshipNameFrom.COLUMN_NAME, is( RelationshipNameFrom.parse( "column" ) ) );
        assertThat( RelationshipNameFrom.TABLE_NAME, is( RelationshipNameFrom.parse( "banana" ) ) );
    }
}
