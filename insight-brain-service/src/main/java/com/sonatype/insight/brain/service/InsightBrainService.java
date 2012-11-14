/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.data.DataResource;
import com.sonatype.insight.brain.legacy.BCResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Environment;

public class InsightBrainService
    extends Service<InsightConfig>
{
    public static void main( final String[] args )
        throws Exception
    {
        new InsightBrainService().run( args );
    }

    @Override
    protected void initialize( final InsightConfig config, final Environment env )
        throws Exception
    {
        config.getSonatypeWork().mkdirs();

        env.addHealthCheck( new InsightHealth( config ) );

        env.addProvider( new InsightWork( config ) );
        env.addProvider( new InsightProxy( config ) );

        env.addResource( DataResource.class );
        env.addResource( ReportResource.class );
        env.addResource( BCResource.class );
    }
}
