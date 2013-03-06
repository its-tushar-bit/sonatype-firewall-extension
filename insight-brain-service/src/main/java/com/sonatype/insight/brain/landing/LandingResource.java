/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path( LandingResource.SERVICE_PATH )
public class LandingResource
{
    public static final String SERVICE_PATH = "";

    @GET
    public Response home()
    {
        return Response.ok().build(); // empty for now
    }
}
