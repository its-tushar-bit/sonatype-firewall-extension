/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.List;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path( LicenseThreatGroupResource.SERVICE_PATH )
public class LicenseThreatGroupResource
{
    public static final String SERVICE_PATH = "rest/licenseThreatGroup/application/{ownerId}";

    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private static String getInternalOwnerId( String ownerId )
    {
        Application application = new ApplicationDAO().getByPublicId( ownerId );
        if ( application != null )
        {
            return application.getId();
        }
        return ownerId;
    }

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<LicenseThreatGroup> getLicenseThreatGroups( @PathParam( "ownerId" ) String ownerId )
    {
        ownerId = getInternalOwnerId( ownerId );

        return licenseThreatGroupDAO.getByOwnerId( ownerId );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroup addLicenseThreatGroup( @PathParam( "ownerId" ) String ownerId,
                                                     LicenseThreatGroup licenseThreatGroup )
    {
        ownerId = getInternalOwnerId( ownerId );

        licenseThreatGroup.setId( null );
        licenseThreatGroup.setOwnerId( ownerId );
        licenseThreatGroupDAO.insert( licenseThreatGroup );

        return licenseThreatGroup;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroup updateLicenseThreatGroup( @PathParam( "ownerId" ) String ownerId,
                                                        LicenseThreatGroup licenseThreatGroup )
    {
        ownerId = getInternalOwnerId( ownerId );

        licenseThreatGroup.setOwnerId( ownerId );
        licenseThreatGroupDAO.update( licenseThreatGroup );

        return licenseThreatGroup;
    }

    @DELETE
    @Path( "{licenseThreatGroupId}" )
    public void deleteLicenseThreatGroup( @PathParam( "ownerId" ) String ownerId,
                                          @PathParam( "licenseThreatGroupId" ) String licenseThreatGroupId )
    {
        ownerId = getInternalOwnerId( ownerId );

        LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getById( licenseThreatGroupId );
        if ( !ownerId.equals( licenseThreatGroup.getOwnerId() ) )
        {
            throw new NotFoundException( "Cannot find a license threat group with id " + licenseThreatGroupId
                + " for owner id " + ownerId );
        }

        licenseThreatGroupDAO.delete( licenseThreatGroup );
    }
}
