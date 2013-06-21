/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtils;
import com.sonatype.insight.brain.report.ReportDownloader.ReportDownloadReponse;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.MediaTypeUtils;
import com.sonatype.insight.client.utils.AuditUtils;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

@Path( ReportResource.SERVICE_PATH )
@Named
public class ReportResource
{
    public static final String SERVICE_PATH = "rest/report/{applicationPublicId}/{scanId}";

    private static final Logger log = LoggerFactory.getLogger( ReportResource.class );

    private static final ConcurrentMap<String, Lock> LOCK_TABLE =
        CacheBuilder.newBuilder().weakValues().<String, Lock> build().asMap();

    private static final long YEAR = 365 * 24 * 60 * 60 * 1000;

    static final ConcurrentMap<String, Integer> MODIFICATION_COUNTS =
        CacheBuilder.newBuilder().maximumSize( 8192 ).<String, Integer> build().asMap();

    @Context
    private InsightWork work;

    @Context
    private BaseUrl baseUrl;

    private ApplicationDAO applicationDAO = new ApplicationDAO();
    
    private final ReportDownloader reportDownloader;

    private final PolicyEvaluationUtils policyEvaluationUtils;

    @Inject
    public ReportResource( final ReportDownloader reportDownloader, final PolicyEvaluationUtils policyEvaluationUtils )
    {
        this.reportDownloader = reportDownloader;
        this.policyEvaluationUtils = policyEvaluationUtils;
    }

    @GET
    @Path( "embedReport/{path:.*}" )
    public Response embedReport( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                 @PathParam( "scanId" ) final String scanId, @PathParam( "path" ) final String path,
                                 @Context final HttpServletRequest httpRequest )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final String name = Report.toEntryName( path );
        final File reportFile = fetchReport( reportDownloader, work, appId, scanId, false );
        ReportEntry reportEntry = null;
        try
        {
            reportEntry = Report.getEntry( reportFile, name );
        }
        catch ( final Exception e )
        {
            log.warn( "Problem embedding report: " + e.getMessage(), e );
        }
        if ( reportEntry != null )
        {
            final long ifModifiedSince = httpRequest.getDateHeader( "If-Modified-Since" );
            if ( ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000 )
            {
                return Response.status( 304 ).build();
            }
            final ResponseBuilder response = Response.ok( reportEntry.buf );
            response.lastModified( new Date( reportEntry.time ) );
            response.type( MediaTypeUtils.byName( name ) );
            if ( !name.endsWith( ".json" ) )
            {
                response.expires( new Date( System.currentTimeMillis() + YEAR ) );
            }
            return response.build();
        }
        return Response.status( Status.NOT_FOUND ).build();
    }

    @GET
    @Path( "reevaluatePolicy" )
    public Response reevaluatePolicy( @PathParam( "applicationPublicId" ) final String applicationPublicId, 
                                      @PathParam( "scanId" ) final String scanId )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();
        PolicyEvaluationLog evalLog = new PolicyEvaluationLog( work.getAuditDir( appId ) );
        PolicyEvaluation policyEvaluation = evalLog.findByScan( scanId );

        if ( policyEvaluation == null )
        {
            throw new BadRequestException( "Policy evaluation for scan " + scanId + " does not exist on the server" );
        }

        policyEvaluationUtils.evaluate( applicationPublicId, scanId, policyEvaluation.getStage() );

        return Response.ok().build();
    }

    @GET
    @Path( "printReport" )
    @Produces( "application/pdf" )
    public Response printReport( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                 @PathParam( "scanId" ) final String scanId,
                                 @QueryParam( "projectName" ) final String projectName,
                                 @QueryParam( "buildNumber" ) final int buildNumber )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final File reportFile = fetchReport( reportDownloader, work, appId, scanId, true );

        final ResponseBuilder response = Response.ok();

        Report.printPdf( reportFile, StringUtils.defaultString( projectName, "clm" ), buildNumber, response );

        return response.build();
    }

    /**
     * @deprecated As of Brain 1.2 (and corresponding SaaS), clients/reports use ComponentInfoResource.
     */
    @GET
    @Path( "artifactDetails{ignore:.*}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response artifactDetails( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                     @PathParam( "scanId" ) final String scanId,
                                     @QueryParam( "groupId" ) final String groupId,
                                     @QueryParam( "artifactId" ) final String artifactId,
                                     @QueryParam( "version" ) final String version,
                                     @Context final HttpServletRequest httpRequest )
        throws Exception
    {
        ReportEntry reportEntry = null;
        try
        {
            Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
            String appId = application.getId();

            final File reportFile =
                fetchReport( reportDownloader, work, appId, scanId, false );
            reportEntry = Report.getEntry( reportFile, "licenses.json" );
            final long ifModifiedSince = httpRequest.getDateHeader( "If-Modified-Since" );
            if ( ifModifiedSince >= 0 && reportEntry.time / 1000 <= ifModifiedSince / 1000 )
            {
                return Response.status( 304 ).build();
            }
        }
        catch ( final Exception e )
        {
            log.debug( "No report available, details will not be augmented", e );
        }
        
        Map<String,String> queryParams = new HashMap<String,String>();
        
        queryParams.put( "groupId", groupId );
        queryParams.put( "artifactId", artifactId );
        queryParams.put( "version", version );
        
        final ReportDownloadReponse result = reportDownloader.fetchReport( "rest/ci/artifact/" + scanId, queryParams );

        final ResponseBuilder response = Response.status( result.getStatusCode() );

        for ( final Entry<String, String> header : result.getHeaders().entrySet() )
        {
            response.header( header.getKey(), header.getValue() );
        }

        final byte[] data;
        if ( result.getStatusCode() < 300 && reportEntry != null )
        {
            data = augmentArtifactDetails( result.getData(), reportEntry.buf );
            response.lastModified( new Date( reportEntry.time ) );
            response.type( "application/json;charset=UTF-8" );
        }
        else
        {
            data = result.getData();
        }

        return response.entity( data ).build();
    }

    @POST
    @Path( "augmentData/{path}" )
    public Response augmentData( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                                 @PathParam( "path" ) final String path, @QueryParam( "user" ) final String user,
                                 @QueryParam( "where" ) final String where, @Context final HttpServletRequest request,
                                 final InputStream stream )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        if ( path.endsWith( ".json" ) && request.getContentLength() > 0 )
        {
            final ContainerNode<?> data;
            try
            {
                data = JsonUtils.parse( IOUtil.toByteArray( stream ) );
            }
            finally
            {
                IOUtil.close( stream );
            }
            final JsonStore store = JsonUtils.fileStore( work.getAuditDir( appId ) );
            store.commit( path, JsonUtils.stamp( user, AuditUtils.findIP( request ), where, data ) );
            return Response.ok().build();
        }
        return Response.status( Status.BAD_REQUEST ).build();
    }

    @GET
    @Path( "auditLog/{path}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response auditLog( @PathParam( "applicationPublicId" ) final String applicationPublicId,
                              @PathParam( "path" ) final String path, @QueryParam( "key" ) final String encodedKey )
        throws IOException
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        final JsonStore store = JsonUtils.fileStore( work.getAuditDir( appId ) );
        final ContainerNode<?> key = encodedKey != null ? JsonUtils.parse( encodedKey.getBytes( "UTF-8" ) ) : null;
        final ContainerNode<?> feed = store.history( key, path.split( "[+]+" ) );
        if ( feed != null )
        {
            return Response.ok( JsonUtils.generate( feed ) ).build();
        }

        return Response.ok().build();
    }

    @GET
    @Path( "brain/{path:.*}" )
    public Response brainGet( final @PathParam( "path" ) String path )
    {
        return redirectToBrain( baseUrl, path );
    }

    @POST
    @Path( "brain/{path:.*}" )
    public Response brainPost( final @PathParam( "path" ) String path )
    {
        return redirectToBrain( baseUrl, path );
    }

    @PUT
    @Path( "brain/{path:.*}" )
    public Response brainPut( final @PathParam( "path" ) String path )
    {
        return redirectToBrain( baseUrl, path );
    }

    @DELETE
    @Path( "brain/{path:.*}" )
    public Response brainDelete( final @PathParam( "path" ) String path )
    {
        return redirectToBrain( baseUrl, path );
    }

    private static Response redirectToBrain( final BaseUrl baseUrl, final String path )
    {
        UriBuilder uriBuilder = baseUrl.redirect().path( path );

        return Response.temporaryRedirect( uriBuilder.build() ).build();
    }

    public static File fetchReport( final ReportDownloader reportDownloader, final InsightWork work, final String appId, final String scanId, final boolean waitForReport )
        throws IOException
    {
        final Lock lock = lockFor( appId, scanId );
        final File reportFile = work.getReportFile( appId, scanId );
        if ( waitForReport || reportFile.exists() )
        {
            lock.lock(); // protect against concurrent download as well as concurrent editing of the report
        }
        else if ( !lock.tryLock() )
        {
            throw new NotFoundException( "The report for scan id " + scanId + " is still being downloaded" );
        }
        try
        {
            if ( !reportFile.exists() )
            {
                int attempts = 0;
                int interval = 0;
                
                if ( waitForReport )
                {
                    attempts = 30;
                    interval = 30;
                }
                final File tempFile = FileUtils.createTempFile( "temp-", ".zip", reportFile.getParentFile() );
                if ( !reportDownloader.downloadReport( scanId, tempFile, attempts, interval ) )
                {
                    throw new NotFoundException( "Could not download the report for scan id " + scanId );
                }
                FileUtils.rename( tempFile, reportFile );
            }

            final File auditDir = work.getAuditDir( appId );
            final int newCount = JsonUtils.fileStore( auditDir ).modificationCount();
            final Integer oldCount = MODIFICATION_COUNTS.get( appId + '-' + scanId );

            if ( oldCount == null || oldCount < newCount )
            {
                Report.deletePdf( reportFile );

                Report.applyChanges( appId, reportFile, auditDir );

                MODIFICATION_COUNTS.put( appId + '-' + scanId, newCount );
            }

            return reportFile;
        }
        finally
        {
            lock.unlock();
        }
    }

    public static void flushReportChanges( final String appId, final String scanId )
    {
        MODIFICATION_COUNTS.remove( appId + '-' + scanId );
    }

    public static void flushReportChanges()
    {
        MODIFICATION_COUNTS.clear();
    }

    private static byte[] augmentArtifactDetails( final byte[] detailData, final byte[] licenseData )
        throws IOException
    {
        byte[] augmentedDetailData = detailData;

        final ObjectNode details = JsonUtils.parse( detailData );

        final ContainerNode<?> licenses = JsonUtils.parse( licenseData );
        final ArrayNode artifacts = (ArrayNode) ( licenses instanceof ArrayNode ? licenses : licenses.get( "aaData" ) );

        final JsonNode overriddenLicenses = getOverriddenLicenses( details, artifacts );
        if ( overriddenLicenses != null )
        {
            details.put( "overriddenLicenses", overriddenLicenses );
            augmentedDetailData = JsonUtils.generate( details );
        }

        return augmentedDetailData;
    }

    private static JsonNode getOverriddenLicenses( final ObjectNode details, final ArrayNode artifacts )
    {
        final String groupId = details.path( "groupId" ).asText();
        final String artifactId = details.path( "artifactId" ).asText();
        final String version = details.path( "version" ).asText();

        for ( int i = 0; i < artifacts.size(); i++ )
        {
            final JsonNode row = artifacts.get( i );
            if ( artifactId.equals( row.path( "artifactId" ).asText() )
                && groupId.equals( row.path( "groupId" ).asText() ) && version.equals( row.path( "version" ).asText() ) )
            {
                return row.get( "overriddenLicenses" );
            }
        }
        return null;
    }

    private static Lock lockFor( final String appId, final String scanId )
    {
        Lock lock = LOCK_TABLE.get( appId + '-' + scanId );
        if ( lock == null )
        {
            final Lock newLock = new ReentrantLock();
            lock = LOCK_TABLE.putIfAbsent( appId + '-' + scanId, newLock );
            if ( lock == null )
            {
                lock = newLock;
            }
        }
        return lock;
    }

    public static String getReportPath( final String appPublicId, final String scanId )
    {
        String url = ReportResource.SERVICE_PATH + "/embedReport/";
        url = url.replace( "{applicationPublicId}", UrlUtils.encodeUrlComponent( appPublicId ) );
        url = url.replace( "{scanId}", UrlUtils.encodeUrlComponent( scanId ) );
        return url;
    }
}
