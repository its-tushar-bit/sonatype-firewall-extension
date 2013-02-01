package com.sonatype.insight.brain.releasegraph;

import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheLoader.ReleaseGraphKey;
import com.yammer.metrics.core.HealthCheck;

public class ReleaseGraphHealthCheck
    extends HealthCheck
{
    private LoadingCache<ReleaseGraphKey, byte[]> cache;

    public ReleaseGraphHealthCheck( LoadingCache<ReleaseGraphKey, byte[]> cache )
    {
        super( "Release Graph" );
        this.cache = cache;
    }

    @Override
    protected Result check()
        throws Exception
    {
        return Result.healthy( "Cache Size - " + cache.size() );
    }
}
