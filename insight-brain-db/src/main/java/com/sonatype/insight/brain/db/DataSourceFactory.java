package com.sonatype.insight.brain.db;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.sonatype.insight.db.AbstractDataSourceFactory;

public class DataSourceFactory
    extends AbstractDataSourceFactory
{
    private static Map<String, DataSource> dataSources = new LinkedHashMap<String, DataSource>();

    @Override
    protected Map<String, DataSource> getDataSources()
    {
        return dataSources;
    }

    @Override
    protected DataSource loadDataSource( File dbConfigFile, String databaseName )
    {
        DataSource dataSource = super.loadDataSource( dbConfigFile, databaseName );
        populateDatabaseSchema( dataSource, databaseName );

        return dataSource;
    }

    /**
     * For tests only
     */
    public static void unloadAll()
    {
        synchronized ( dataSources )
        {
            dataSources.clear();
        }
    }
}
