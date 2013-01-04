/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.upload.BOMCheckReportDownloadRequest;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadRequest;
import com.sonatype.insight.scan.upload.BOMCheckScanUploadResult;
import com.sonatype.insight.scan.upload.DefaultReportDownloader;
import com.sonatype.insight.scan.upload.DefaultScanUploader;
import com.sonatype.insight.scan.upload.ReportDownloader;
import com.sonatype.insight.scan.upload.ScanUploader;

@Path( BCResource.SERVICE_PATH )
public class BCResource
{
    public static final String SERVICE_PATH = "rest/bc";

    private static final Logger log = LoggerFactory.getLogger( BCResource.class );

    final ScanUploader uploader = new DefaultScanUploader( log, false );

    final ReportDownloader downloader = new DefaultReportDownloader( log );

    @Context
    InsightWork work;

    @Context
    InsightProxy proxy;

    @GET
    @Path( "validate/{appId}" )
    @Produces( MediaType.TEXT_PLAIN )
    public String validateToken( @PathParam( "appId" ) final String appId )
        throws Exception
    {
        final BOMCheckScanUploadRequest request = new BOMCheckScanUploadRequest( appId, null, null );

        return uploader.validateToken( proxy.contextualize( request ) );
    }

    @PUT
    @Path( "scan/{appId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response putScan( @PathParam( "appId" ) final String appId,
                             @QueryParam( "instanceId" ) final String instanceId,
                             @QueryParam( "jobId" ) final String jobId, final InputStream data )
        throws Exception
    {
        final BOMCheckScanUploadRequest request = new BOMCheckScanUploadRequest( appId, null, null );
        final File scanFile = FileUtils.createTempFile( "temp-", ".xml.gz", work.getScanDir( appId ) );
        final File scanDir = scanFile.getParentFile();

        scanDir.mkdirs();
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
        if ( StringUtils.isNotBlank( result.getScanId() ) )
        {
            FileUtils.rename( scanFile, new File( scanDir, "scan-" + result.getScanId() + ".xml.gz" ) );
        }

        return Response.ok( result ).build();
    }

    @GET
    @Path( "report/{appId}" )
    @Produces( MediaType.APPLICATION_OCTET_STREAM )
    public StreamingOutput getReport( @PathParam( "appId" ) final String appId,
                                      @QueryParam( "scanId" ) final String scanId )
    {
        final BOMCheckReportDownloadRequest request = new BOMCheckReportDownloadRequest( appId, scanId, null );

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
                                     @QueryParam( "version" ) final String version )
    {
        return Response.temporaryRedirect( URI.create( "rest/report/" + work.findOwningAppId( scanId ) + '/' + scanId //
            + "/artifactDetails" + "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version ) ).build();
    }
}
