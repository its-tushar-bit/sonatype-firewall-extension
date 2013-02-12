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
        return client.doProxy( path, req );
    }

    @GET
    @Path( "details/{path:.*}" )
    public Response getDetailsResource( @PathParam( "path" ) String path, @Context HttpServletRequest req )
        throws IOException
    {
        return client.doProxy( path, req );
    }
}
