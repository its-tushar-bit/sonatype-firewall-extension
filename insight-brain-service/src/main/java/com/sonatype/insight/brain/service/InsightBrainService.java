/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.google.common.cache.CacheBuilderSpec;
import com.sonatype.insight.brain.data.DataResource;
import com.sonatype.insight.brain.legacy.BCResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluatorResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.rule.ActionTypeResource;
import com.sonatype.insight.brain.rule.ConditionTypeResource;
import com.sonatype.insight.brain.rule.RuleResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.bundles.AssetsBundle;
import com.yammer.dropwizard.config.Environment;

public class InsightBrainService
    extends Service<InsightConfig>
{
    public static void main( final String[] args )
        throws Exception
    {
        new InsightBrainService().run( args.length > 0 ? args : new String[] { "server" } );
    }

    public InsightBrainService()
    {
        super( "insight-brain-service" );

        final CacheBuilderSpec cacheSpec = AssetsBundle.DEFAULT_CACHE_SPEC;
        addBundle( new AssetsBundle( "/com/sonatype/insight/brain/rules/assets/", cacheSpec, "/rule-assets/" ) );
    }

    @Override
    protected void initialize( final InsightConfig config, final Environment env )
        throws Exception
    {
        config.getSonatypeWork().mkdirs();

        env.addHealthCheck( new InsightHealth( config ) );

        env.addProvider( new InsightWork( config ) );
        env.addProvider( new InsightProxy( config ) );

        env.addResource( ActionTypeResource.class );
        env.addResource( ConditionTypeResource.class );
        env.addResource( DataResource.class );
        env.addResource( PolicyEvaluatorResource.class );
        env.addResource( ReportResource.class );
        env.addResource( RuleResource.class );
        env.addResource( BCResource.class );
    }
}
