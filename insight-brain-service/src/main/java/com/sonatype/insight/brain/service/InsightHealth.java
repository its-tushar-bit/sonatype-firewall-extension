/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.yammer.metrics.core.HealthCheck;

public class InsightHealth
    extends HealthCheck
{
    private final InsightConfig insightConfig;

    public InsightHealth( final InsightConfig insightConfig )
    {
        super( "insightBrainService" );
        this.insightConfig = insightConfig;
    }

    @Override
    protected Result check()
        throws Exception
    {
        if ( !insightConfig.getSonatypeWork().isDirectory() )
        {
            return Result.unhealthy( insightConfig.getSonatypeWork() + " is not a directory" );
        }
        return Result.healthy();
    }
}
