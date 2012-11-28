/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.ning.http.client.Response;

public abstract class AbstractResourceTest
{
    private static TestInsightBrainService service;

    @Before
    public void startService()
        throws Exception
    {
        if ( service == null )
        {
            service = new TestInsightBrainService();
            service.run( new String[] { "server" } );
        }
    }

    @After
    public void stopService()
        throws Exception
    {
        if ( service != null )
        {
            service.stop();
            service = null;
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
