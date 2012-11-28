/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import net.sourceforge.argparse4j.inf.Namespace;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.cli.ServerCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.ServerFactory;
import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;

public class TestInsightBrainService
    extends InsightBrainService
{
    /**
     * Copied from com.yammer.dropwizard.cli.ServerCommand only to be able to keep a reference to the server instance
     * that is started by the command.
     */
    private static final class TestServerCommand<T extends Configuration>
        extends ServerCommand<T>
    {
        private static final Logger log = LoggerFactory.getLogger( TestServerCommand.class );

        private Server server;

        public TestServerCommand( Service<T> service )
        {
            super( service );
        }

        @Override
        protected void run( Environment environment, Namespace namespace, T configuration )
            throws Exception
        {
            server =
                new ServerFactory( configuration.getHttpConfiguration(), environment.getName() ).buildServer( environment );
            try
            {
                server.start();
                final ServerLifecycleListener listener = environment.getServerListener();
                if ( listener != null )
                {
                    listener.serverStarted( server );
                }
            }
            catch ( Exception e )
            {
                log.error( "Unable to start server, shutting down", e );
                server.stop();
            }
        }
    }

    private TestServerCommand<InsightConfig> testServerCommand;

    @Override
    public void initialize( Bootstrap<InsightConfig> bootstrap )
    {
        super.initialize( bootstrap );
        testServerCommand = new TestServerCommand<InsightConfig>( this );
        bootstrap.addCommand( testServerCommand );
    }

    public void stop()
        throws Exception
    {
        testServerCommand.server.stop();
    }
}
