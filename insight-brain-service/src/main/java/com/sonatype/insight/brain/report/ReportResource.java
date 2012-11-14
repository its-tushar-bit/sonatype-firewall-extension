/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.data.Auditing;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckReportDownloadRequest;
import com.sonatype.insight.scan.upload.DefaultReportDownloader;
import com.sonatype.insight.scan.upload.ReportDataRequest;
import com.sonatype.insight.scan.upload.ReportDataResult;
import com.sonatype.insight.scan.upload.ReportDownloader;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import eu.medsea.mimeutil.detector.ExtensionMimeDetector;

@Path( "/rest/report/{appId}/{scanId}" )
public class ReportResource
{
    private static final Logger log = LoggerFactory.getLogger( ReportResource.class );

    private static final MimeUtil2 mimeUtil = new MimeUtil2();

    static
    {
        mimeUtil.registerMimeDetector( ExtensionMimeDetector.class.getName() );
    }

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
        if ( Auditing.isData( name ) || !work.getReportFile( scanId ).exists() )
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
        if ( entry != null )
        {
            final MimeType mimeType = MimeUtil2.getMostSpecificMimeType( mimeUtil.getMimeTypes( name ) );
            return Response.ok( entry.buf ).type( mimeType.toString() ).build();
        }
        return Response.status( Status.NOT_FOUND ).build();
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
                    if ( !downloadReport( appId, scanId, reportFile ) )
                    {
                        return;
                    }
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

    private boolean downloadReport( final String appId, final String scanId, final File reportFile )
    {
        final BOMCheckReportDownloadRequest request = new BOMCheckReportDownloadRequest( appId, scanId, null );

        request.setRetryAttempts( 30 );
        request.setRetryInterval( 30 );

        reportFile.getAbsoluteFile().getParentFile().mkdirs();
        try
        {
            final OutputStream os = new BufferedOutputStream( new FileOutputStream( reportFile ) );
            try
            {
                new DefaultReportDownloader( log ).download( proxy.contextualize( request ), os );
                return true;
            }
            finally
            {
                IOUtil.close( os );
            }
        }
        catch ( final Exception e )
        {
            // don't leave an incomplete file around
            reportFile.delete();
        }
        return false;
    }
}
