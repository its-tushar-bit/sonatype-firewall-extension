package com.sonatype.insight.clm.report;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.clm.data.Auditing;
import com.sonatype.insight.clm.service.InsightProxy;
import com.sonatype.insight.clm.service.InsightWork;
import com.sonatype.insight.scan.upload.DefaultReportDownloader;
import com.sonatype.insight.scan.upload.ReportDataRequest;
import com.sonatype.insight.scan.upload.ReportDataResult;
import com.sonatype.insight.scan.upload.ReportDownloader;

@Path( "/rest/report/{appId}/{scanId}" )
public class ReportResource
{
    private static final Logger log = LoggerFactory.getLogger( ReportResource.class );

    final ReportDownloader downloader = new DefaultReportDownloader( log );

    @Context
    InsightWork work;

    @Context
    InsightProxy proxy;

    @GET
    @Path( "html/{entryPath:.*}" )
    public Response getHTML( @PathParam( "appId" ) final String appId, @PathParam( "scanId" ) final String scanId,
                             @PathParam( "entryPath" ) final String entryPath )
    {
        final String name = Report.toEntryName( entryPath );
        if ( Auditing.isData( name ) )
        {
            refreshCache( appId, scanId );
        }
        ReportEntry entry = null;
        try
        {
            entry = Report.getEntry( work.getReportFile( scanId ), name );
        }
        catch ( final Exception e )
        {
            log.warn( "Embedding error", e );
        }
        return entry != null ? Response.ok( entry.buf ).build() : Response.status( Status.NOT_FOUND ).build();
    }

    @GET
    @Path( "pdf" )
    @Produces( "application/pdf" )
    public Response getPDF( @PathParam( "appId" ) final String appId, @PathParam( "scanId" ) final String scanId )
        throws IOException
    {
        refreshCache( appId, scanId );

        final ResponseBuilder response = Response.ok();

        Report.printPdf( log, work.getReportFile( scanId ), "Insight", 0, response ); // FIXME: pass in job title

        return response.build();
    }

    @GET
    @Path( "artifact" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getArtifact( @PathParam( "scanId" ) final String scanId,
                                 @QueryParam( "groupId" ) final String groupId,
                                 @QueryParam( "artifactId" ) final String artifactId,
                                 @QueryParam( "version" ) final String version )
        throws Exception
    {
        final ReportDataRequest request = new ReportDataRequest( "rest/bc/artifact/" + scanId + //
            "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version, null );

        final ReportDataResult result = downloader.fetch( proxy.contextualize( request ) );

        final ResponseBuilder response = Response.status( result.getStatusCode() );

        for ( final Entry<String, String> header : result.getHeaders().entrySet() )
        {
            response.header( header.getKey(), header.getValue() );
        }

        return response.entity( result.getData() ).build();
    }

    private void refreshCache( final String appId, final String scanId )
    {
        try
        {
            Auditing.getModificationCount( work.getAuditDir( appId ) );
            if ( true /* FIXME: should only refresh when necessary */)
            {
                final File reportFile = work.getReportFile( scanId );
                if ( !reportFile.exists() )
                {
                    return;
                }

                Report.deletePdf( log, reportFile );

                Report.applyChanges( reportFile, work.getAuditDir( appId ) );
            }
        }
        catch ( final Exception e )
        {
            log.warn( "Could not apply latest data edits to Insight report", e );
        }
    }
}
