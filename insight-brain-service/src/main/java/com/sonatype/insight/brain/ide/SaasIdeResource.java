/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path( SaasIdeResource.PATH )
public class SaasIdeResource
{
    public static final String PATH = "rest/ide/";

    @Context
    private SaasClient client;

    @GET
    @Path( "cip/{path:.*}" )
    public Response getCipResource( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "ide", path );
    }

    @GET
    @Path( "details/{appId}/{path:.*}" )
    public Response getDetailsResource( @PathParam( "path" ) String path,
                                        @PathParam( "appId" ) String applicationPublicId,
                                        @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/artifact/detail/", applicationPublicId, path );
    }

    @GET
    @Path( "artifact/{path:.*}" )
    public Response getArtifactInfo( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/artifact/", path );
    }

    @GET
    @Path( "scan/{path:.*}" )
    public Response doScan( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( req, "rest/ide/scan", path );
    }
}