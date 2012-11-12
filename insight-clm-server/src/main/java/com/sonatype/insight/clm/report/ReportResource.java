package com.sonatype.insight.clm.report;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.clm.legacy.BCResource;
import com.sonatype.insight.clm.service.InsightProxy;
import com.sonatype.insight.scan.upload.DefaultReportDownloader;
import com.sonatype.insight.scan.upload.ReportDataRequest;
import com.sonatype.insight.scan.upload.ReportDataResult;
import com.sonatype.insight.scan.upload.ReportDownloader;

@Path( "/rest/report/{appId}/{scanId}" )
public class ReportResource
{
    private static final Logger log = LoggerFactory.getLogger( BCResource.class );

    final ReportDownloader downloader = new DefaultReportDownloader( log );

    @Context
    InsightProxy proxy;

    @GET
    @Path( "html" )
    public Response getHTML()
    {
        throw new UnsupportedOperationException();
    }

    @GET
    @Path( "pdf" )
    public Response getPDF()
    {
        throw new UnsupportedOperationException();
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
}
