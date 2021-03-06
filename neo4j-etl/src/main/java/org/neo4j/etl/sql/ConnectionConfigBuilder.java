package org.neo4j.etl.sql;

class ConnectionConfigBuilder implements ConnectionConfig.Builder.SetHost,
        ConnectionConfig.Builder.SetPort,
        ConnectionConfig.Builder.SetDatabaseOrUsername,
        ConnectionConfig.Builder.SetUsername,
        ConnectionConfig.Builder.SetPassword,
        ConnectionConfig.Builder

{
    final DatabaseType databaseType;
    String host;
    int port;
    String database = "";
    String username;
    String password;

    ConnectionConfigBuilder( DatabaseType databaseType )
    {
        this.databaseType = databaseType;
    }

    @Override
    public ConnectionConfig.Builder.SetPort host( String host )
    {
        this.host = host;
        return this;
    }

    @Override
    public SetDatabaseOrUsername port( int port )
    {
        this.port = port;
        return this;
    }

    @Override
    public SetUsername database( String database )
    {
        this.database = database;
        return this;
    }

    @Override
    public ConnectionConfig.Builder.SetPassword username( String username )
    {
        this.username = username;
        return this;
    }

    @Override
    public ConnectionConfig.Builder password( String password )
    {
        this.password = password;
        return this;
    }

    @Override
    public ConnectionConfig build()
    {
        return new ConnectionConfig( this );
    }
}
