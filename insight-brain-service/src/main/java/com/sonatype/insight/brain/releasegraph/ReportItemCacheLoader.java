package com.sonatype.insight.brain.releasegraph;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.json.store.JsonUtils;

public class ReportItemCacheLoader
    extends CacheLoader<ReportItemKey, ReportPopularity>
{

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Override
    public ReportPopularity load( ReportItemKey key )
        throws Exception
    {
        Application application = applicationDAO.getByPublicIdNotNull( key.getApplicationPublicId() );
        String appId = application.getId();

        final String name = Report.toEntryName( "popularity.json" );
        final File reportFile =
            ReportResource.fetchReport( key.getWork(), key.getProxy(), key.getApplicationPublicId(), appId,
                                        key.getScanId(), false );
        ReportEntry reportEntry = null;
        try
        {
            reportEntry = Report.getEntry( reportFile, name );
        }
        catch ( final Exception e )
        {
            log.warn( "Problem embedding report: {}", e.getMessage(), e );
            throw e;
        }

        if ( reportEntry == null )
        {
            log.error( "popularity.json file is missing from report for scan {}", key.getScanId() );
            throw new IllegalStateException( "popularity.json is missing from report" );
        }
        return JsonUtils.parse( reportEntry.buf, ReportPopularity.class );
    }
}
