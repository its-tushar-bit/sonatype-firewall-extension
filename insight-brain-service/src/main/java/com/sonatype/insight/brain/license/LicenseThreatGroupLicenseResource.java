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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

@Path( LicenseThreatGroupLicenseResource.SERVICE_PATH )
public class LicenseThreatGroupLicenseResource
{
    public static final String SERVICE_PATH = "rest/licensethreatgrouplicense/application/{applicationPublicId}";

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @Path( "{licenseThreatGroupId}" )
    public List<LicenseThreatGroupLicense> getLicenseThreatGroupLicenses( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                                          @PathParam( "licenseThreatGroupId" ) String licenseThreatGroupId )
    {
        applicationDAO.getByPublicIdNotNull( applicationPublicId );

        return licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId( licenseThreatGroupId );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroupLicense addLicenseThreatGroupLicense( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                                   LicenseThreatGroupLicense licenseThreatGroupLicense )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        licenseThreatGroupLicense.setId( null );
        licenseThreatGroupLicense.setApplicationId( appId );
        licenseThreatGroupLicenseDAO.insert( licenseThreatGroupLicense );

        return licenseThreatGroupLicense;
    }

    @DELETE
    @Path( "{licenseThreatGroupLicenseId}" )
    public void deleteLicenseThreatGroupLicense( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                 @PathParam( "licenseThreatGroupLicenseId" ) String licenseThreatGroupLicenseId )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        LicenseThreatGroupLicense licenseThreatGroupLicense =
            licenseThreatGroupLicenseDAO.getById( licenseThreatGroupLicenseId );
        if ( licenseThreatGroupLicense == null || !appId.equals( licenseThreatGroupLicense.getApplicationId() ) )
        {
            throw new NotFoundException( "Cannot find a license threat group license with id "
                + licenseThreatGroupLicenseId + " for application id " + applicationPublicId );
        }

        licenseThreatGroupLicenseDAO.delete( licenseThreatGroupLicense );
    }
}
