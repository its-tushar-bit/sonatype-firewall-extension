/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.label.LabelResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluatorResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.rule.ActionTypeResource;
import com.sonatype.insight.brain.rule.ConditionTypeResource;
import com.sonatype.insight.brain.rule.RuleResource;
import com.sonatype.insight.brain.saas.BCResource;
import com.sonatype.insight.db.DatabaseConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class InsightBrainService
    extends Service<InsightConfig>
{
    public static void main( final String[] args )
        throws Exception
    {
        new InsightBrainService().run( args.length > 0 ? args : new String[] { "server" } );
    }

    @Override
    public void initialize( final Bootstrap<InsightConfig> bootstrap )
    {
        bootstrap.addBundle( new AssetsBundle( "/com/sonatype/insight/brain/rules/assets/", "/rule-assets/" ) );
        bootstrap.addBundle( new AssetsBundle( "/com/sonatype/insight/brain/policy/assets/", "/policy-assets/" ) );
    }

    @Override
    public void run( final InsightConfig config, final Environment env )
        throws Exception
    {
        config.getSonatypeWork().mkdirs();

        env.enableJerseyFeature( ResourceConfig.FEATURE_CANONICALIZE_URI_PATH );
        env.enableJerseyFeature( ResourceConfig.FEATURE_NORMALIZE_URI );

        env.addHealthCheck( new InsightHealth( config ) );

        env.addProvider( new InsightWork( config ) );
        env.addProvider( new InsightProxy( config ) );

        DatabaseConfig databaseConfig = new DatabaseConfig( config.getConfigDir() );
        OperationalDataStoreProvider.init( databaseConfig );

        env.addResource( ActionTypeResource.class );
        env.addResource( ConditionTypeResource.class );
        env.addResource( LabelResource.class );
        env.addResource( PolicyEvaluatorResource.class );
        env.addResource( ReportResource.class );
        env.addResource( RuleResource.class );
        env.addResource( BCResource.class );
    }
}
