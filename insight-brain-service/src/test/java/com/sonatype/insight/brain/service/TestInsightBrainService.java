/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.apache.commons.cli.CommandLine;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.AbstractService;
import com.yammer.dropwizard.cli.ServerCommand;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.ServerFactory;

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

        public TestServerCommand( Class<T> configurationClass )
        {
            super( configurationClass );
        }

        @Override
        protected void run( AbstractService<T> service, T configuration, CommandLine params )
            throws Exception
        {
            final Environment environment = new Environment( service, configuration );
            service.initializeWithBundles( configuration, environment );
            server =
                new ServerFactory( configuration.getHttpConfiguration(), service.getName() ).buildServer( environment );
            try
            {
                server.start();
            }
            catch ( Exception e )
            {
                log.error( "Unable to start server, shutting down", e );
                server.stop();
            }
        }
    }

    private TestServerCommand<InsightConfig> testServerCommand;

    public TestInsightBrainService()
    {
        testServerCommand = new TestServerCommand<InsightConfig>( getConfigurationClass() );
        addCommand( testServerCommand );
    }

    public void stop()
        throws Exception
    {
        testServerCommand.server.stop();
    }
    // public static InsightBrainService getService()
    // {
    // InsightBrainService service = new InsightBrainService();
    //
    // service._addCommand( command );
    // return service;
    // }
}
