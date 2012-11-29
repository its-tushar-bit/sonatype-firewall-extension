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
import org.junit.Assert;
import org.junit.Before;

import com.ning.http.client.Response;
import com.sonatype.insight.mock.InsightMockServer;

public abstract class AbstractResourceTest
{
    private static int saasPort = findFreePort( 8071 );

    private static int brainPort = findFreePort( 8070 );

    private static File saasWork = new File( "target/mock-saas-work/" );

    private InsightMockServer saas;

    private TestInsightBrainService brain;

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

    protected static File getJsonResponseDirectory()
    {
        return new File( saasWork, "json" );
    }

    protected static File getZipResponseDirectory()
    {
        return new File( saasWork, "zip" );
    }

    protected static File getReportResponseFile( String appId, String scanId )
    {
        return new File( getZipResponseDirectory(), appId + '-' + scanId + ".zip" );
    }

    protected static void assertResponseStatus( int expectedStatus, Response response )
        throws IOException
    {
        int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText()
            + ", ResponseBody:" + response.getResponseBody(), expectedStatus, actualStatus );
    }

    protected static int findFreePort( int defaultPort )
    {
        int port = defaultPort;
        ServerSocket socket = null;
        try
        {
            socket = new ServerSocket( 0 );
            port = socket.getLocalPort();
        }
        catch ( IOException e )
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
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
        return port;
    }
}
