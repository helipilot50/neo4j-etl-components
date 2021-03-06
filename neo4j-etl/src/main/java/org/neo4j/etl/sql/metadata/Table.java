package org.neo4j.etl.sql.metadata;

import java.util.Collection;

import org.apache.commons.lang3.builder.ToStringBuilder;

import org.neo4j.etl.util.Preconditions;

public class Table implements DatabaseObject
{
    public static Builder.SetName builder()
    {
        return new TableBuilder();
    }

    private final TableName name;
    private final Collection<Column> columns;

    Table( TableBuilder builder )
    {
        this.name = Preconditions.requireNonNull( builder.table, "Name" );
        this.columns = Preconditions.requireNonNull( builder.columns, "Columns" );
    }

    public TableName name()
    {
        return name;
    }

    public Collection<Column> columns()
    {
        return columns;
    }

    @Override
    public String descriptor()
    {
        return name.fullName();
    }

    @Override
    public <T> T invoke( DatabaseObjectServiceProvider<T> databaseObjectServiceProvider )
    {
        return databaseObjectServiceProvider.tableService( this );
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString( this );
    }

    public interface Builder
    {
        interface SetName
        {
            Builder name( String name );

            Builder name( TableName name );
        }

        Builder addColumn( Column column );

        Table build();
    }
}
