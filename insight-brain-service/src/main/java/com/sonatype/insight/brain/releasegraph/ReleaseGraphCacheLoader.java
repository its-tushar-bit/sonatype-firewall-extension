package com.sonatype.insight.brain.releasegraph;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.model.GAVPopularity;
import com.sonatype.insight.brain.model.ReportPopularity;

public class ReleaseGraphCacheLoader
    extends CacheLoader<ReleaseGraphKey, byte[]>
{
    private LoadingCache<ReportItemKey, ReportPopularity> cache =
        CacheBuilder.newBuilder().expireAfterAccess( 5, TimeUnit.MINUTES ).build( new ReportItemCacheLoader() );

    @Override
    public byte[] load( ReleaseGraphKey key )
        throws Exception
    {
        ReportPopularity reportPopularity = cache.get( key.getReportItemKey() );
        for ( GAVPopularity pop : reportPopularity.getPopularity() )
        {
            if ( key.isMatch( pop ) )
            {
                ReleaseGraph graph =
                    new ReleaseGraph( ReleaseGraphModel.build( pop, reportPopularity.getFirstCatalog(),
                                                               reportPopularity.getLastCatalog(),
                                                               ReleaseGraphModel.SLOTS ), ReleaseGraphModel.SLOTS );
                return graph.getBytes();
            }
        }
        throw new IllegalArgumentException( "No match for GAV" );
    }
}
