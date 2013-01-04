/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.label.ComponentLabelResource;
import com.sonatype.insight.brain.label.LabelResource;
import com.sonatype.insight.brain.policy.ActionTypeResource;
import com.sonatype.insight.brain.policy.ConditionTypeResource;
import com.sonatype.insight.brain.policy.ConditionValueTypeResource;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.policy.StageTypeResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.saas.BCResource;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.JaxRsExceptionMapper;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;

public class InsightBrainService
    extends Service<InsightConfig>
{
    private static final Logger log = LoggerFactory.getLogger( InsightBrainService.class );

    public static void main( final String[] args )
        throws Exception
    {
        new InsightBrainService().run( args.length > 0 ? args : new String[] { "server" } );
    }

    @Override
    public void initialize( final Bootstrap<InsightConfig> bootstrap )
    {
        bootstrap.addBundle( new AssetsBundle( "/com/sonatype/insight/brain/policy/assets/", "/policy-assets/" ) );
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

        env.addProvider( new InsightWork( config ) );
        env.addProvider( new InsightProxy( config ) );

        DatabaseConfig databaseConfig = new DatabaseConfig( config.getConfigDir() );
        OperationalDataStoreProvider.init( databaseConfig );

        env.addResource( ComponentLabelResource.class );
        env.addResource( LabelResource.class );
        env.addResource( ActionTypeResource.class );
        env.addResource( ConditionTypeResource.class );
        env.addResource( ConditionValueTypeResource.class );
        env.addResource( StageTypeResource.class );
        env.addResource( PolicyEvaluateResource.class );
        env.addResource( PolicyResource.class );
        env.addResource( ReportResource.class );
        env.addResource( BCResource.class );
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
