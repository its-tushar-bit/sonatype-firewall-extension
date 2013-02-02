package com.sonatype.insight.brain.releasegraph;

import java.io.PrintWriter;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

public class ReleaseGraphTask
    extends Task
{
    private LoadingCache<ReleaseGraphKey, byte[]> cache;

    public ReleaseGraphTask( LoadingCache<ReleaseGraphKey, byte[]> cache )
    {
        super( "clearReleaseGraphCache" );
        this.cache = cache;
    }

    @Override
    public void execute( ImmutableMultimap<String, String> parameters, PrintWriter output )
        throws Exception
    {
        output.write( "Starting cache size: " + cache.size() );
        cache.invalidateAll();
        output.write( "\nFinal cache size: " + cache.size() );
    }
}
