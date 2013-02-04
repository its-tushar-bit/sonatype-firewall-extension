package com.sonatype.insight.brain.releasegraph;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.HttpStatusCode;

@Path( "rest/report/releaseGraph" )
public class ReleaseGraphResource
{
    private static final Logger log = LoggerFactory.getLogger( ReleaseGraphResource.class );

    @Context
    private InsightWork work;

    @Context
    private InsightProxy proxy;

    private final LoadingCache<ReleaseGraphKey, byte[]> cache;

    public ReleaseGraphResource( LoadingCache<ReleaseGraphKey, byte[]> cache )
    {
        this.cache = cache;
    }

    @GET
    public Response getImage( @QueryParam( "applicationId" ) final String applicationPublicId,
                              @QueryParam( "scanId" ) final String scanId, @QueryParam( "groupId" ) String groupId,
                              @QueryParam( "artifactId" ) String artifactId, @QueryParam( "version" ) String version )
    {
        log.debug( "Creating popularity graph for {}:{}:{} for scan {}", groupId, artifactId, version, scanId );
        try
        {
            return Response.ok( cache.get( new ReleaseGraphKey( groupId, artifactId, version,
                                                                new ReportItemKey( applicationPublicId, scanId, work,
                                                                                   proxy ) ) ), "image/png" ).build();
        }
        catch ( Exception e )
        {
            // undo any wrapping of resource exceptions introduced by Guava caches
            for ( Throwable t = e; t instanceof RuntimeException; t = t.getCause() )
            {
                if ( t.getClass().isAnnotationPresent( HttpStatusCode.class ) || t instanceof WebApplicationException )
                {
                    throw (RuntimeException) t;
                }
            }

            throw new RuntimeException( "Error creating popularity graph for " + groupId + ":" + artifactId + ":"
                + version
                + " for report " + scanId, e );
        }
    }
}
