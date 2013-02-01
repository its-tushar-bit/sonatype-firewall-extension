package com.sonatype.insight.brain.releasegraph;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.GAVPopularity;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheLoader.ReleaseGraphKey;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

public class ReleaseGraphCacheLoader
    extends CacheLoader<ReleaseGraphKey, byte[]>
{

    public static class ReleaseGraphKey
    {
        private String artifactId;

        private String groupId;

        private String version;

        private String applicationPublicId;

        private String scanId;

        private InsightWork work;

        private InsightProxy proxy;

        public ReleaseGraphKey( String groupId, String artifactId, String version,
                              String applicationPublicId, String scanId, InsightWork work, InsightProxy proxy )
        {
            super();
            this.artifactId = artifactId;
            this.groupId = groupId;
            this.version = version;
            this.applicationPublicId = applicationPublicId;
            this.scanId = scanId;
            this.work = work;
            this.proxy = proxy;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( applicationPublicId == null ) ? 0 : applicationPublicId.hashCode() );
            result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
            result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
            result = prime * result + ( ( scanId == null ) ? 0 : scanId.hashCode() );
            result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
            return result;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( obj == this )
            {
                return true;
            }
            if ( !( obj instanceof ReleaseGraphKey ) )
            {
                return false;
            }
            ReleaseGraphKey that = (ReleaseGraphKey) obj;
            return eq( scanId, that.scanId ) && eq( applicationPublicId, that.applicationPublicId )
                && eq( artifactId, that.artifactId ) && eq( groupId, that.groupId ) && eq( version, that.version );
        }

        private boolean isMatch( GAVPopularity gav )
        {
            return eq( artifactId, gav.getArtifactId() ) && eq( groupId, gav.getGroupId() )
                && eq( version, gav.getVersion() );
        }

        private static <T> boolean eq( T o1, T o2 )
        {
            return ( o1 != null ) ? o1.equals( o2 ) : o2 == null;
        }
    }

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Override
    public byte[] load( ReleaseGraphKey key )
        throws Exception
    {
        // TODO Auto-generated method stub
        int slots = 50;
        Application application = applicationDAO.getByPublicIdNotNull( key.applicationPublicId );
        String appId = application.getId();

        final String name = Report.toEntryName( "popularity.json" );
        final File reportFile =
            ReportResource.fetchReport( key.work, key.proxy, key.applicationPublicId, appId, key.scanId, false );
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
            log.error( "popularity.json file is missing from report for scan {}", key.scanId );
            throw new IllegalStateException( "popularity.json is missing from report" );
        }
        ReportPopularity reportPopularity = JsonUtils.parse( reportEntry.buf, ReportPopularity.class );
        for ( GAVPopularity pop : reportPopularity.getPopularity() )
        {
            if ( key.isMatch( pop ) )
            {
                ReleaseGraph graph =
                    new ReleaseGraph( ReleaseGraphModel.build( pop, reportPopularity.getFirstCatalog(),
                                                                     reportPopularity.getLastCatalog(), slots ), slots );
                return graph.getBytes();
            }
        }
        throw new IllegalArgumentException( "No match for GAV" );
    }
}
