/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.eclipse.jetty.server.Server;

import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;

public class TestInsightBrainService
    extends InsightBrainService
{
    int testPort;

    String testSaasAddress;

    Server testBrainServer;

    Exception brainFault;

    public void setHttpPort( int port )
    {
        testPort = port;
    }

    public void setSaasAddress( String saasAddress )
    {
        testSaasAddress = saasAddress;
    }

    public void start()
        throws Exception
    {
        if ( testBrainServer != null )
        {
            throw new IllegalStateException( "Brain server already started" );
        }

        new Thread()
        {
            @Override
            public void run()
            {
                brainFault = null;
                try
                {
                    // this method will only return when the service is stopped...
                    TestInsightBrainService.this.run( new String[] { "server" } );
                }
                catch ( Exception e )
                {
                    brainFault = e;
                }
            }
        }.start();

        // Warning: must set correct test port *before* any use of RestAccess!
        System.setProperty( "insight-app-port", Integer.toString( testPort ) );

        final String testURL = RestAccess.BASE_REST_URL + "/bc/validate/freemium"; // low-cost service

        long start = System.currentTimeMillis();
        Exception serverStartException = null;
        for ( int retries = 0; retries < 60 * 20; retries++ )
        {
            try
            {
                Thread.sleep( 50 );
                if ( RestAccess.get( testURL ).getStatusCode() == 200 )
                {
                    serverStartException = null;
                    break;
                }
            }
            catch ( Exception e )
            {
                // server is still booting...
                serverStartException = e;
            }
        }
        if ( serverStartException != null )
        {
            throw serverStartException;
        }
        System.out.println( "Detected server started in " + ( System.currentTimeMillis() - start ) );
    }

    @Override
    public void run( InsightConfig config, Environment env )
        throws Exception
    {
        config.getHttpConfiguration().setPort( testPort );
        config.getHttpConfiguration().setAdminPort( testPort );
        config.setSaasAddress( testSaasAddress );
        env.setServerLifecycleListener( new TestServerListener( env.getServerListener() ) );
        super.run( config, env );
    }

    public void stop()
        throws Exception
    {
        if ( testBrainServer != null )
        {
            testBrainServer.stop();
            testBrainServer = null;
        }
        if ( brainFault != null )
        {
            throw brainFault;
        }
    }

    private class TestServerListener
        implements ServerLifecycleListener
    {
        ServerLifecycleListener delegate;

        TestServerListener( ServerLifecycleListener delegate )
        {
            this.delegate = delegate;
        }

        @Override
        public void serverStarted( Server server )
        {
            if ( delegate != null )
            {
                delegate.serverStarted( server );
            }
            testBrainServer = server;
        }
    }
}
