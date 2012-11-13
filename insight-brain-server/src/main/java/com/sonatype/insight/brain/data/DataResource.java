package com.sonatype.insight.brain.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.plexus.util.StringUtils;

import com.sonatype.insight.brain.service.InsightWork;

@Path( "/rest/data/{appId}/{name}" )
public class DataResource
{
    @Context
    InsightWork work;

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    public Response putData( @PathParam( "appId" ) final String appId, @PathParam( "name" ) final String name,
                             @QueryParam( "user" ) final String user, @QueryParam( "where" ) final String where,
                             @Context final HttpServletRequest request, final InputStream data )
        throws IOException
    {
        if ( Auditing.isData( name ) )
        {
            final File auditDir = work.getAuditDir( appId );
            Auditing.saveAugmentedData( auditDir, name, data, user, Auditing.findIP( request ), where );
            return Response.ok().build();
        }
        return Response.status( Status.BAD_REQUEST ).build();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getData( @PathParam( "appId" ) final String appId, @PathParam( "name" ) final String name,
                             @QueryParam( "key" ) final String key )
        throws IOException
    {
        if ( StringUtils.isNotBlank( key ) )
        {
            final File auditDir = work.getAuditDir( appId );
            final byte[] buf = Auditing.filterAuditLog( auditDir, key.getBytes( "UTF-8" ), name.split( "[+]+" ) );
            if ( buf != null )
            {
                return Response.ok( buf ).build();
            }
        }
        return Response.ok().build();
    }
}
