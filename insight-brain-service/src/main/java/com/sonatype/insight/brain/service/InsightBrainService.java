/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.application.ApplicationResource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.features.FeaturesResource;
import com.sonatype.insight.brain.ide.SaasIdeResource;
import com.sonatype.insight.brain.label.ComponentLabelResource;
import com.sonatype.insight.brain.label.LabelResource;
import com.sonatype.insight.brain.landing.LandingResource;
import com.sonatype.insight.brain.license.LicenseResource;
import com.sonatype.insight.brain.license.LicenseThreatGroupLicenseResource;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource;
import com.sonatype.insight.brain.policy.ActionTypeResource;
import com.sonatype.insight.brain.policy.ConditionTypeResource;
import com.sonatype.insight.brain.policy.ConditionValueTypeResource;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.policy.StageTypeResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheLoader;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphHealthCheck;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphKey;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphResource;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphTask;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.saas.CIResource;
import com.sonatype.insight.brain.saas.ComponentInfoResource;
import com.sonatype.insight.brain.saas.EnvironmentResource;
import com.sonatype.insight.brain.saas.RepoManResource;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.version.VersionResource;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.JaxRsExceptionMapper;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

public class InsightBrainService
    extends Service<InsightConfig>
{
    private static final Logger log = LoggerFactory.getLogger( InsightBrainService.class );

    private static final String POLICY_ASSET_PATH = "/policy-assets/";

    private static final String BRAIN_ASSET_PATH = "/assets/";

    public static void main( final String[] args )
        throws Exception
    {
        new InsightBrainService().run( args.length > 0 ? args : new String[]{ "server" } );
    }

    @Override
    public void initialize( final Bootstrap<InsightConfig> bootstrap )
    {
        bootstrap.addBundle( new AssetsBundle( "/com/sonatype/insight/brain/policy/assets/", POLICY_ASSET_PATH ) );
        bootstrap.addBundle( new AssetsBundle( "/com/sonatype/insight/brain/assets/", BRAIN_ASSET_PATH ) );
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
    public void run( final InsightConfig config, final Environment env )
        throws Exception
    {
        replaceGenericExceptionMapper( env );

        config.getSonatypeWork().mkdirs();

        env.enableJerseyFeature( ResourceConfig.FEATURE_CANONICALIZE_URI_PATH );
        env.enableJerseyFeature( ResourceConfig.FEATURE_NORMALIZE_URI );

        env.addHealthCheck( new InsightHealth( config ) );

        InsightProxy proxy = new InsightProxy( config );
        env.addProvider( new InsightWork( config ) );
        env.addProvider( proxy );
        env.addProvider( new InsightMail( config ) );
        env.addProvider( new SaasClient( proxy ) );

        File databaseDir = new File( config.getSonatypeWork(), "data" );
        DatabaseConfig dmDatabaseConfig = getDatabaseConfig( databaseDir, "dm" );
        DatamartProvider.init( dmDatabaseConfig );
        DatabaseConfig odsDatabaseConfig = getDatabaseConfig( databaseDir, "ods" );
        OperationalDataStoreProvider.init( odsDatabaseConfig );
        loadDatabase();

        env.addResource( ApplicationResource.class );
        env.addResource( FeaturesResource.class );
        env.addResource( ComponentLabelResource.class );
        env.addResource( LabelResource.class );
        env.addResource( LicenseResource.class );
        env.addResource( LicenseThreatGroupResource.class );
        env.addResource( LicenseThreatGroupLicenseResource.class );
        env.addResource( ActionTypeResource.class );
        env.addResource( ConditionTypeResource.class );
        env.addResource( ConditionValueTypeResource.class );
        env.addResource( StageTypeResource.class );
        env.addResource( PolicyEvaluateResource.class );
        env.addResource( PolicyResource.class );
        env.addResource( ReportResource.class );
        env.addResource( CIResource.class );
        env.addResource( RepoManResource.class );
        env.addResource( VersionResource.class );
        env.addResource( SaasIdeResource.class );
        env.addResource( ComponentInfoResource.class );
        env.addResource( EnvironmentResource.class );

        LoadingCache<ReleaseGraphKey, byte[]> cache =
            CacheBuilder.newBuilder().maximumSize( config.getReleaseGraphCacheSize() ).build(
                new ReleaseGraphCacheLoader() );
        env.addResource( new ReleaseGraphResource( cache ) );
        env.addHealthCheck( new ReleaseGraphHealthCheck( cache ) );
        env.addTask( new ReleaseGraphTask( cache ) );

        env.addResource( LandingResource.class );
    }

    private void loadDatabase()
    {
        // not very nice, but good enough for now
        // Load the datamart db:
        new LicenseDAO().getById( "UNSPECIFIED" );
        // Load the ODS db:
        new ApplicationDAO().getById( "not a real id" );
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
}
