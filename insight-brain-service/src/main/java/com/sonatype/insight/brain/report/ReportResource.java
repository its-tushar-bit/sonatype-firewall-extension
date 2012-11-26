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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.data.Auditing;
import com.sonatype.insight.brain.data.DataStore;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.client.utils.AuditUtils;
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
    @Path( "embedReport/{path:.*}" )
    public Response embedReport( @PathParam( "appId" ) final String appId, @PathParam( "scanId" ) final String scanId,
                                 @PathParam( "path" ) final String path )
    {
        final String name = Report.toEntryName( path );
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
    @Path( "printReport" )
    @Produces( "application/pdf" )
    public Response printReport( @PathParam( "appId" ) final String appId, @PathParam( "scanId" ) final String scanId,
                                 @QueryParam( "projectName" ) final String projectName,
                                 @QueryParam( "buildNumber" ) final int buildNumber )
        throws IOException
    {
        refreshCache( appId, scanId );

        final ResponseBuilder response = Response.ok();

        Report.printPdf( work.getReportFile( scanId ), StringUtils.defaultString( projectName, "insight" ),
                         buildNumber, response );

        return response.build();
    }

    @GET
    @Path( "artifactDetails{ignore:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response artifactDetails( @PathParam( "scanId" ) final String scanId,
                                     @QueryParam( "groupId" ) final String groupId,
                                     @QueryParam( "artifactId" ) final String artifactId,
                                     @QueryParam( "version" ) final String version,
                                     @Context final HttpServletRequest httpRequest )
        throws Exception
    {
        ReportEntry reportEntry = null;

        final File reportFile = work.getReportFile( scanId );
        if ( reportFile.exists() )
        {
            reportEntry = Report.getEntry( reportFile, "licenses.json" );
            final long ifModifiedSince = httpRequest.getDateHeader( "If-Modified-Since" );
            if ( ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000 )
            {
                return Response.status( 304 ).build();
            }
        }

        final ReportDataRequest request = new ReportDataRequest( "rest/bc/artifact/" + scanId + //
            "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version, null );

        final ReportDataResult result = downloader.fetch( proxy.contextualize( request ) );

        final ResponseBuilder response = Response.status( result.getStatusCode() );

        for ( final Entry<String, String> header : result.getHeaders().entrySet() )
        {
            response.header( header.getKey(), header.getValue() );
        }

        final byte[] data;
        if ( result.getStatusCode() < 300 && reportEntry != null )
        {
            data = DataStore.augmentArtifactDetails( result.getData(), reportEntry.buf );
            response.lastModified( new Date( reportEntry.time ) );
            response.type( "application/json; charset=UTF-8" );
        }
        else
        {
            data = result.getData();
        }

        return response.entity( data ).build();
    }

    @POST
    @Path( "augmentData/{path}" )
    public Response augmentData( @PathParam( "appId" ) final String appId, @PathParam( "path" ) final String path,
                                 @QueryParam( "user" ) final String user, @QueryParam( "where" ) final String where,
                                 @Context final HttpServletRequest request, final InputStream data )
        throws IOException
    {
        if ( Auditing.isData( path ) )
        {
            final File auditDir = work.getAuditDir( appId );
            Auditing.saveAugmentedData( auditDir, path, data, user, AuditUtils.findIP( request ), where );
            return Response.ok().build();
        }
        return Response.status( Status.BAD_REQUEST ).build();
    }

    @GET
    @Path( "auditLog/{path}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response auditLog( @PathParam( "appId" ) final String appId, @PathParam( "path" ) final String path,
                              @QueryParam( "key" ) final String key )
        throws IOException
    {
        if ( StringUtils.isNotBlank( key ) )
        {
            final File auditDir = work.getAuditDir( appId );
            final byte[] buf = Auditing.filterAuditLog( auditDir, key.getBytes( "UTF-8" ), path.split( "[+]+" ) );
            if ( buf != null )
            {
                return Response.ok( buf ).build();
            }
        }
        return Response.ok().build();
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
                    if ( !downloadReport( proxy, appId, scanId, reportFile ) )
                    {
                        return;
                    }
                }

                Report.deletePdf( reportFile );

                Report.applyChanges( reportFile, work.getAuditDir( appId ) );
            }
        }
        catch ( final Exception e )
        {
            log.warn( "Could not apply latest data edits to Insight report", e );
        }
    }

    public static boolean downloadReport( final InsightProxy proxy, final String appId, final String scanId,
                                          final File reportFile )
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
            log.error( e.getMessage(), e );
            reportFile.delete();
        }
        return false;
    }
}
