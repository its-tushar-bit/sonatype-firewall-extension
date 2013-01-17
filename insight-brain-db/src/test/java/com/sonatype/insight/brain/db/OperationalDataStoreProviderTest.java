/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.insight.db.DatabaseConfig;

public class OperationalDataStoreProviderTest
{
    @Before
    public void setUp()
        throws Exception
    {
        DataSourceFactory.unloadAll();
    }

    @After
    public void tearDown()
    {
        DataSourceFactory.unloadAll();
    }

    @Test
    public void verifyDatabaseCreation_InMemory()
        throws Exception
    {
        verifyDatabaseCreation( null /* databaseConfig */);
    }

    @Test
    public void verifyDatabaseCreation_OnDisk()
        throws Exception
    {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setDriverClassName( "org.h2.Driver" );
        databaseConfig.setUrl( "jdbc:h2:target/OperationalDataStoreProviderTest/test" );
        databaseConfig.setUsername( "sa" );
        databaseConfig.setPassword( "" );
        databaseConfig.setMaxConnections( 50 );
        File databaseDir = new File( "target/OperationalDataStoreProviderTest" );
        FileUtils.deleteDirectory( databaseDir );

        // New database
        verifyDatabaseCreation( databaseConfig );
        Assert.assertTrue( databaseDir.exists() );
        Assert.assertTrue( new File( databaseDir, "test.h2.db" ).exists() );

        // Existing database
        DataSourceFactory.unloadAll();
        verifyDatabaseCreation( databaseConfig );
        Assert.assertTrue( databaseDir.exists() );
        Assert.assertTrue( new File( databaseDir, "test.h2.db" ).exists() );
    }

    private void verifyDatabaseCreation( DatabaseConfig databaseConfig )
        throws Exception
    {
        OperationalDataStoreProvider.init( databaseConfig );
        DataSource dataSource = OperationalDataStoreProvider.get();
        Assert.assertNotNull( dataSource );
        Connection conn = dataSource.getConnection();
        try
        {
            exec( conn, "SELECT * FROM test_table" );
        }
        finally
        {
            conn.close();
        }
    }

    private void exec( Connection conn, String sql )
        throws SQLException
    {
        Statement stmt = conn.createStatement();
        try
        {
            stmt.execute( sql );
        }
        finally
        {
            if ( stmt != null )
            {
                stmt.close();
            }
        }
    }
}
