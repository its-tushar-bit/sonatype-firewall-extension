/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.yammer.metrics.core.HealthCheck;

/**
 * This is a Dropwizard health check. It has nothing to do with Insight's Health Check features like "Application Health
 * Check" or "Repository Health Check". :)
 */
public class InsightHealth
    extends HealthCheck
{
    private final InsightConfig insightConfig;

    public InsightHealth( final InsightConfig insightConfig )
    {
        super( "insight-brain-service" );
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
