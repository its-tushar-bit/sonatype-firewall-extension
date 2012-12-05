/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.mock.InsightMockServer;

public abstract class AbstractBrainServiceTest
{
    private static int saasPort = findFreePort( 8071 );

    private static int brainPort = findFreePort( 8070 );

    private static File saasWork = new File( "target/mock-saas-work/" );

    private InsightMockServer saas;

    protected TestInsightBrainService brain;

    @BeforeClass
    public static void beforeClass()
    {
        DatabaseConfig databaseConfig = new DatabaseConfig( null /* configDir */);
        OperationalDataStoreProvider.init( databaseConfig );
    }

    @AfterClass
    public static void afterClass()
    {
        DataSourceFactory.unloadAll();
    }

    @Before
    public void startService()
        throws Exception
    {
        if ( saas == null )
        {
            saas = new InsightMockServer();
            saas.setHttpPort( saasPort );
            saas.setJsonResponseDirectory( getJsonResponseDirectory() );
            saas.setZipResponseDirectory( getZipResponseDirectory() );
            saas.start();
        }
        if ( brain == null )
        {
            brain = new TestInsightBrainService();
            brain.setHttpPort( brainPort );
            brain.setSaasAddress( saas.getHttpUrl() );
            brain.start();
        }
    }

    @After
    public void stopService()
        throws Exception
    {
        if ( brain != null )
        {
            brain.stop();
            brain = null;
        }
        if ( saas != null )
        {
            saas.stop();
            saas = null;
        }
    }

    protected void invalidateAppId( final String appId, final String reason )
    {
        saas.invalidateAppId( appId, reason );
    }

    protected static File getJsonResponseDirectory()
    {
        return new File( saasWork, "json" );
    }

    protected static File getZipResponseDirectory()
    {
        return new File( saasWork, "zip" );
    }

    protected static File getScanResponseFile( final String appId )
    {
        return new File( getJsonResponseDirectory(), appId + ".json" );
    }

    protected static File getReportResponseFile( final String appId, final String scanId )
    {
        return new File( getZipResponseDirectory(), appId + '-' + scanId + ".zip" );
    }

    protected static int findFreePort( final int defaultPort )
    {
        int port = defaultPort;
        ServerSocket socket = null;
        try
        {
            socket = new ServerSocket( 0 );
            port = socket.getLocalPort();
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
        }
        finally
        {
            if ( socket != null )
            {
                try
                {
                    socket.close();
                }
                catch ( final IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        return port;
    }
}
