/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            stopAsyncAppenders();
        }
    }

    protected static void assertResponseStatus( int expectedStatus, Response response )
        throws IOException
    {
        int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText()
            + ", ResponseBody:" + response.getResponseBody(), expectedStatus, actualStatus );
    }

    /*
     * Workaround for DropWizard 0.5.1 restart bug... (fixed in latest DW snapshot)
     */
    private void stopAsyncAppenders()
    {
        try
        {
            final ch.qos.logback.classic.Logger log =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME );

            final Field running = com.yammer.dropwizard.logging.AsyncAppender.class.getDeclaredField( "running" );
            final Field dispatcher = com.yammer.dropwizard.logging.AsyncAppender.class.getDeclaredField( "dispatcher" );

            running.setAccessible( true );
            dispatcher.setAccessible( true );

            final Iterator<? extends ch.qos.logback.core.Appender<?>> itr = log.iteratorForAppenders();
            while ( itr.hasNext() )
            {
                final ch.qos.logback.core.Appender<?> appender = itr.next();
                if ( appender instanceof com.yammer.dropwizard.logging.AsyncAppender )
                {
                    running.setBoolean( appender, false );
                    final Thread t = ( (Thread) dispatcher.get( appender ) );
                    t.interrupt();
                }
            }
        }
        catch ( final Exception e )
        {
            LoggerFactory.getLogger( getClass() ).warn( "Cannot interrupt AsyncAppender", e );
        }
    }
}
