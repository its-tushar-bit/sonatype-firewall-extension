/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

public class OperationalDataStoreProvider
{
    public static final String ID = "insight_brain_ods";

    private static DatabaseConfig config;

    private static DataSourceFactory factory = new DataSourceFactory();

    private OperationalDataStoreProvider()
    {
    }

    public static void init( DatabaseConfig databaseConfig )
    {
        config = databaseConfig;
    }

    public static synchronized DataSource get()
    {
        return factory.newDataSource( config, ID );
    }
}
