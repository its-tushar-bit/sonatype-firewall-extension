/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;

public class TestInsightBrainService
    extends InsightBrainService
{
    private static final Logger log = LoggerFactory.getLogger( TestInsightBrainService.class );

    private int testPort;

    private String testSaasAddress;

    private Server testBrainServer;

    private Exception brainFault;

    public void setHttpPort( final int port )
    {
        testPort = port;
    }

    public void setSaasAddress( final String saasAddress )
    {
        testSaasAddress = saasAddress;
    }

    public Configuration getClientConfiguration()
    {
        final Configuration configuration = new Configuration();
        configuration.setServerUrl( "http://localhost:" + testPort );
        return configuration;
    }

    public void start()
        throws Exception
    {
        if ( testBrainServer != null )
        {
            throw new IllegalStateException( "Brain server already started" );
        }

        // Warning: must set correct test port *before* any use of RestAccess!
        System.setProperty( "insight-app-port", Integer.toString( testPort ) );

        new Thread( "TestInsightBrainService" )
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
                catch ( final Exception e )
                {
                    log.error( e.getMessage(), e );
                    brainFault = e;
                }
            }
        }.start();

        final Configuration configuration = getClientConfiguration();
        final StatusClient client = new StatusClient( configuration );

        final long start = System.currentTimeMillis();
        Exception serverStartException = null;
        for ( int retries = 0; retries < 60 * 20; retries++ )
        {
            if ( brainFault != null )
            {
                throw brainFault;
            }
            try
            {
                Thread.sleep( 50 );
                if ( client.check() )
                {
                    serverStartException = null;
                    break;
                }
            }
            catch ( final Exception e )
            {
                // server is still booting...
                serverStartException = e;
            }
        }
        if ( serverStartException != null )
        {
            log.error( serverStartException.getMessage(), serverStartException );
            throw serverStartException;
        }
        System.out.println( "Detected server started in " + ( System.currentTimeMillis() - start ) );
    }

    @Override
    public void run( final InsightConfig config, final Environment env )
        throws Exception
    {
        config.getHttpConfiguration().setPort( testPort );
        config.getHttpConfiguration().setAdminPort( testPort );
        config.setSonatypeWork( "target/test-brain-work" );
        config.setSaasAddress( testSaasAddress );

        FileUtils.deleteDirectory( config.getSonatypeWork() );

        env.addServerLifecycleListener( new ServerLifecycleListener()
        {
            @Override
            public void serverStarted( final Server server )
            {
                testBrainServer = server;
            }
        } );
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

    private static class StatusClient
        extends AbstractClient
    {
        StatusClient( final Configuration configuration )
        {
            super( configuration );
        }

        public boolean check()
            throws Exception
        {
            return path( "rest/bc/validate/freemium" ).get().status() == 200;
        }
    }
}
