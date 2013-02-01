package com.sonatype.insight.brain.service;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheLoader.ReleaseGraphKey;

@Path( "rest/report/releaseGraph" )
public class ReleaseGraphService
{


    private Logger log = LoggerFactory.getLogger( getClass() );

    @Context
    InsightWork work;

    @Context
    InsightProxy proxy;

    private final LoadingCache<ReleaseGraphKey, byte[]> cache;

    public ReleaseGraphService( LoadingCache<ReleaseGraphKey, byte[]> cache )
    {
        this.cache = cache;
    }

    @GET
    public Response getImage( @QueryParam( "applicationId" ) final String applicationPublicId,
                              @QueryParam( "scanId" ) final String scanId, @QueryParam( "groupId" ) String groupId,
                              @QueryParam( "artifactId" ) String artifactId, @QueryParam( "version" ) String version )
        throws Exception
    {
        log.debug( "Creating popularity graph for " + groupId + ":" + artifactId + ":" + version + " for report "
            + scanId );
        try
        {
            return Response.ok( cache.get( new ReleaseGraphKey( groupId, artifactId, version, applicationPublicId,
                                                                scanId, work, proxy ) ), "image/png" ).build();
        }
        catch ( Exception e )
        {
            throw new Exception( "Error creating popularity graph for " + groupId + ":" + artifactId + ":" + version
                + " for report " + scanId, e );
        }
    }
}
