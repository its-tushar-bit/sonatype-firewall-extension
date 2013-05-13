/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheLoader;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphKey;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.JaxRsExceptionMapper;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;

@Named
public class InsightBrainService
    extends SisuService<InsightConfig>
{
    private static final Logger log = LoggerFactory.getLogger( InsightBrainService.class );

    static
    {
        // INSIGHT-4557
        System.setProperty( "java.awt.headless", "true" );
    }

    public static final String APPLICATION_ASSET_PATH = "/application-assets/";

    public static final String BRAIN_ASSET_PATH = "/assets/";

    public static final String POLICY_ASSET_PATH = "/policy-assets/";

    public static final String UNLICENSED_ASSET_PATH = "/unlicensed-assets/";

    public static void main( final String[] args )
        throws Exception
    {
        new InsightBrainService().run( args.length > 0 ? args : new String[] { "server" } );
    }

    @Override
    public void initialize( final Bootstrap<InsightConfig> bootstrap )
    {
        bootstrap.addBundle( new AssetsBundle( "/assets/application/", APPLICATION_ASSET_PATH, "index.html" ) );
        bootstrap.addBundle( new AssetsBundle( "/assets/assets/", BRAIN_ASSET_PATH ) );
        bootstrap.addBundle( new AssetsBundle( "/assets/policy/", POLICY_ASSET_PATH, "index.html" ) );
        bootstrap.addBundle( new AssetsBundle( "/assets/unlicensed/", UNLICENSED_ASSET_PATH, "index.html" ) );

        // workaround to let us set different defaults in the core HTTP configuration
        bootstrap.getObjectMapperFactory().registerModule( new HttpConfig.Module() );
    }

    protected DatabaseConfig getDatabaseConfig( File databaseDir, String databaseName )
    {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setDriverClassName( "org.h2.Driver" );
        databaseConfig.setUrl( "jdbc:h2:" + databaseDir.getAbsolutePath() + '/' + databaseName
            + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000" );
        databaseConfig.setUsername( "sa" );
        databaseConfig.setPassword( "" );
        databaseConfig.setMaxConnections( 50 );
        return databaseConfig;
    }

    @Override
    protected void customize( final InsightConfig config, final Environment env )
    {
        replaceGenericExceptionMapper( env );

        config.getSonatypeWork().mkdirs();

        env.enableJerseyFeature( ResourceConfig.FEATURE_CANONICALIZE_URI_PATH );
        env.enableJerseyFeature( ResourceConfig.FEATURE_NORMALIZE_URI );

        log.info( "Server base URL: {}", config.getBaseUrl() );
        log.debug( "Saas address: {}", config.getSaasAddress() );
        log.debug( "Headless mode: {}", java.awt.GraphicsEnvironment.isHeadless() );
    }

    // Copied from IdeScanService
    private void replaceGenericExceptionMapper( final Environment environment )
    {
        // DW has an exception mapper that turns exceptions into 500. Boo for us.
        // Remove it so that our mapper will always be used to handle exceptions.
        final Set<Object> singletons = environment.getJerseyResourceConfig().getSingletons();
        for ( Object candidate : singletons )
        {
            if ( candidate instanceof LoggingExceptionMapper )
            {
                log.debug( "Removing LoggingExceptionMapper" );
                singletons.remove( candidate );
                break;
            }
        }

        // Add our own mapper for exceptions.
        environment.addProvider( new JaxRsExceptionMapper() );
    }

    @Override
    protected List<Module> modules( final InsightConfig config )
    {
        // NOTE: The ReleaseGraphCacheLoader indirectly uses the ApplicationDAO so we better setup the DB before
        File databaseDir = new File( config.getSonatypeWork(), "data" );
        DatabaseConfig dmDatabaseConfig = getDatabaseConfig( databaseDir, "dm" );
        DatamartProvider.init( dmDatabaseConfig );
        DatabaseConfig odsDatabaseConfig = getDatabaseConfig( databaseDir, "ods" );
        OperationalDataStoreProvider.init( odsDatabaseConfig );

        return Arrays.<Module> asList( new AbstractModule()
        {
            @Override
            protected void configure()
            {
                final LoadingCache<ReleaseGraphKey, byte[]> cache =
                    CacheBuilder.newBuilder().maximumSize( config.getReleaseGraphCacheSize() ).build( new ReleaseGraphCacheLoader() );
                bind( new TypeLiteral<LoadingCache<ReleaseGraphKey, byte[]>>()
                {
                } ).toInstance( cache );
            }
        } );
    }
}
