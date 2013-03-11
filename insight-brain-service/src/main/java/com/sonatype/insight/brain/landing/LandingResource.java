/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.service.InsightBrainService;

@Path( LandingResource.SERVICE_PATH )
public class LandingResource
{
    public static final String SERVICE_PATH = "";

    @GET
    public Response home()
    {
        return Response.seeOther( URI.create( InsightBrainService.APPLICATION_ASSET_PATH + "index.html" ) ).build();
    }
}
