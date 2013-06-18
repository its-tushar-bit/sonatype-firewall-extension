/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.saas.SaasResourceClient;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Associates component hash to a Maven GAV.
 * 
 * @since 1.4.1
 */
@Named
@Path( HashGAVResource.SERVICE_PATH )
public class HashGAVResource
{
    public static final String SERVICE_PATH = "rest/component/identified";

    private SaasResourceClient client;

    @Inject
    public HashGAVResource( SaasResourceClient saasResourceClient )
    {
        this.client = saasResourceClient;
    }

    /**
     * @since 1.4.1
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public HashGAV setHashGAV( HashGAV hashGAV )
        throws IOException
    {
        ComponentSummary componentSummary = client.getComponentSummary( hashGAV.getCoordinates() );

        if ( componentSummary.isKnown() )
        {
            throw new BadRequestException( "The '" + hashGAV.getGAVECString() + "' coordinates are already in use" );
        }
        
        hashGAV.setId( null );
        new HashGAVDAO().insert( hashGAV );

        return hashGAV;
    }
}
