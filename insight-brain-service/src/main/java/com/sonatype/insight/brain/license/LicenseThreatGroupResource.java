/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.List;

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

@Path( LicenseThreatGroupResource.SERVICE_PATH )
public class LicenseThreatGroupResource
{
    public static final String SERVICE_PATH = "rest/licenseThreatGroup/application/{applicationPublicId}";

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<LicenseThreatGroup> getLicenseThreatGroups( @PathParam( "applicationPublicId" ) String applicationPublicId )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        return licenseThreatGroupDAO.getByApplicationId( application.getId() );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroup addLicenseThreatGroup( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                     LicenseThreatGroup licenseThreatGroup )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        licenseThreatGroup.setId( null );
        licenseThreatGroup.setApplicationId( appId );
        licenseThreatGroupDAO.insert( licenseThreatGroup );

        return licenseThreatGroup;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroup updateLicenseThreatGroup( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                        LicenseThreatGroup licenseThreatGroup )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        licenseThreatGroup.setApplicationId( appId );
        licenseThreatGroupDAO.update( licenseThreatGroup );

        return licenseThreatGroup;
    }

    @DELETE
    @Path( "{licenseThreatGroupId}" )
    public void deleteLicenseThreatGroup( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                          @PathParam( "licenseThreatGroupId" ) String licenseThreatGroupId )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getById( licenseThreatGroupId );
        if ( !appId.equals( licenseThreatGroup.getApplicationId() ) )
        {
            throw new NotFoundException( "Cannot find a license threat group with id " + licenseThreatGroupId
                + " for application id " + applicationPublicId );
        }

        licenseThreatGroupDAO.delete( licenseThreatGroup );
    }
}
