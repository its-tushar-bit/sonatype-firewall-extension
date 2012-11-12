package com.sonatype.insight.clm.data;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.clm.legacy.BCResource;

@Path( "/rest/data/{appId}/{name}" )
public class DataResource
{
    private static final Logger log = LoggerFactory.getLogger( BCResource.class );

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    public Response putData( @QueryParam( "user" ) final String user, @QueryParam( "where" ) final String where )
    {
        throw new UnsupportedOperationException();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getData( @QueryParam( "key" ) final String key )
    {
        throw new UnsupportedOperationException();
    }
}
