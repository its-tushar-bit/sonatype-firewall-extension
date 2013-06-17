/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;

@Named
@Path( HashGAVResource.SERVICE_PATH )
public class HashGAVResource
{
    public static final String SERVICE_PATH = "rest/component/identified";

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public HashGAV setHashGAV( HashGAV hashGAV )
    {
        hashGAV.setId( null );
        new HashGAVDAO().insert( hashGAV );

        return hashGAV;
    }
}
