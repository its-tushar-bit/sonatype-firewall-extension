/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.ning.http.client.Response;
import com.sonatype.insight.mock.InsightMockServer;

public abstract class AbstractResourceTest
{
    private static InsightMockServer saas;

    private static TestInsightBrainService brain;

    @Before
    public void startService()
        throws Exception
    {
        if ( saas == null )
        {
            saas = new InsightMockServer();
            saas.setHttpPort( 9000 );
            saas.setJsonResponseDirectory( new File( "src/test/resources/json" ) );
            saas.setZipResponseDirectory( new File( "src/test/resources/zip" ) );
            saas.start();
        }
        if ( brain == null )
        {
            brain = new TestInsightBrainService();
            brain.run( new String[] { "server" } );
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

    protected static void assertResponseStatus( int expectedStatus, Response response )
        throws IOException
    {
        int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText()
            + ", ResponseBody:" + response.getResponseBody(), expectedStatus, actualStatus );
    }
}
