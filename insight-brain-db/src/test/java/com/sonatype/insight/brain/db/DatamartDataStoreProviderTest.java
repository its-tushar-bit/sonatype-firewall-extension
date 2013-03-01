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

public class DatamartDataStoreProviderTest
{
    @Before
    public void setUp()
        throws Exception
    {
        DataSourceFactory.clear_ForTestsOnly();
    }

    @After
    public void tearDown()
    {
        DataSourceFactory.clear_ForTestsOnly();
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
        databaseConfig.setUrl( "jdbc:h2:target/DatamartDataStoreProviderTest/test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000" );
        databaseConfig.setUsername( "sa" );
        databaseConfig.setPassword( "" );
        databaseConfig.setMaxConnections( 50 );
        File databaseDir = new File( "target/DatamartDataStoreProviderTest" );
        FileUtils.deleteDirectory( databaseDir );

        // New database
        verifyDatabaseCreation( databaseConfig );
        Assert.assertTrue( databaseDir.exists() );
        Assert.assertTrue( new File( databaseDir, "test.h2.db" ).exists() );

        // Existing database
        DataSourceFactory.clear_ForTestsOnly();
        verifyDatabaseCreation( databaseConfig );
        Assert.assertTrue( databaseDir.exists() );
        Assert.assertTrue( new File( databaseDir, "test.h2.db" ).exists() );
    }

    private void verifyDatabaseCreation( DatabaseConfig databaseConfig )
        throws Exception
    {
        DatamartProvider.init( databaseConfig );
        DataSource dataSource = DatamartProvider.getDataSource();
        Assert.assertNotNull( dataSource );
        Connection conn = dataSource.getConnection();
        try
        {
            exec( conn, "SELECT * FROM test_table" );

            String databaseURL = conn.getMetaData().getURL();
            Assert.assertNotNull( databaseURL );
            if ( databaseConfig != null )
            {
                Assert.assertTrue( databaseConfig.getUrl().startsWith( databaseURL + ";" ) );
            }
            else
            {
                Assert.assertEquals( "jdbc:h2:mem:inMemoryDatabase", databaseURL );
            }
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
