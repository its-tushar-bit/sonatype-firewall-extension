/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hudson;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

/**
 * The client-side UI makes requests to "/crumbIssuer/api/xml" to detect and integrate with Hudson's XSRF protection.
 * When the UI runs outside of Hudson, this resource prevents irritating 404 errors in the CLM server log.
 */
@Named
@Path( CrumbIssuerStubResource.SERVICE_PATH )
@UnlicensedPath
public class CrumbIssuerStubResource
{
    public static final String SERVICE_PATH = "crumbIssuer/api/xml";

    @GET
    public Response get()
    {
        return Response.status( Status.NOT_FOUND ).build();
    }
}
