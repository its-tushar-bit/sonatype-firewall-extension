/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legacy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.data.DataStore;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckReportDownloadRequest;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadRequest;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;
import com.sonatype.insight.scan.upload.DefaultReportDownloader;
import com.sonatype.insight.scan.upload.DefaultScanUploader;
import com.sonatype.insight.scan.upload.ReportDataRequest;
import com.sonatype.insight.scan.upload.ReportDataResult;
import com.sonatype.insight.scan.upload.ReportDownloader;
import com.sonatype.insight.scan.upload.ScanUploader;

@Path( "/rest/bc" )
public class BCResource
{
    private static final Logger log = LoggerFactory.getLogger( BCResource.class );

    final ScanUploader uploader = new DefaultScanUploader( log, false );

    final ReportDownloader downloader = new DefaultReportDownloader( log );

    @Context
    InsightWork work;

    @Context
    InsightProxy proxy;

    @GET
    @Path( "validate/{token}" )
    @Produces( MediaType.TEXT_PLAIN )
    public String validateToken( @PathParam( "token" ) final String token )
        throws Exception
    {
        final BOMCheckScanUploadRequest request = new BOMCheckScanUploadRequest( token, null, null );

        return uploader.validateToken( proxy.contextualize( request ) ) ? "OK" : "FAILED";
    }

    @PUT
    @Path( "scan/{token}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response putScan( @PathParam( "token" ) final String token,
                             @QueryParam( "instanceId" ) final String instanceId,
                             @QueryParam( "jobId" ) final String jobId, final InputStream data )
        throws Exception
    {
        final BOMCheckScanUploadRequest request = new BOMCheckScanUploadRequest( token, null, null );

        final File scanFile = File.createTempFile( "insight-brain", "xml" ); // FIXME

        final FileOutputStream os = new FileOutputStream( scanFile );
        try
        {
            IOUtil.copy( data, os );
        }
        finally
        {
            IOUtil.close( os );
        }

        request.setScanFile( scanFile );
        request.setInstanceId( instanceId );
        request.setJobId( jobId );

        final BOMCheckScanUploadResult result = uploader.upload( proxy.contextualize( request ) );

        return Response.ok( result ).build();
    }

    @GET
    @Path( "report/{token}" )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    public StreamingOutput getReport( @PathParam( "token" ) final String token,
                                      @QueryParam( "scanId" ) final String scanId )
    {
        final BOMCheckReportDownloadRequest request = new BOMCheckReportDownloadRequest( token, scanId, null );

        return new StreamingOutput()
        {
            @Override
            public void write( final OutputStream os )
                throws IOException
            {
                try
                {
                    downloader.download( proxy.contextualize( request ), os );
                }
                catch ( final InterruptedException e )
                {
                    throw new IOException( e.getMessage() );
                }
            }
        };
    }

    @GET
    @Path( "artifact/{scanId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getArtifactInfo( @PathParam( "scanId" ) final String scanId,
                                     @QueryParam( "groupId" ) final String groupId,
                                     @QueryParam( "artifactId" ) final String artifactId,
                                     @QueryParam( "version" ) final String version,
                                     @Context final HttpServletRequest httpRequest )
        throws Exception
    {
        final long ifModifiedSince = httpRequest.getDateHeader( "If-Modified-Since" );
        final ReportEntry reportEntry = Report.getEntry( work.getReportFile( scanId ), "licenses.json" );
        if ( ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000 )
        {
            return Response.status( 304 ).build();
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
        if ( result.getStatusCode() < 300 )
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
}
