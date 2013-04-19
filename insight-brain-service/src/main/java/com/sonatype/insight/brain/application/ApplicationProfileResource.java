/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

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

import com.sonatype.insight.brain.dataaccess.ApplicationProfileDAO;
import com.sonatype.insight.brain.model.ApplicationProfile;

@Named
@Path( ApplicationProfileResource.SERVICE_PATH )
public class ApplicationProfileResource
{
    public static final String SERVICE_PATH = "rest/applicationProfile";

    private ApplicationProfileDAO applicationProfileDAO = new ApplicationProfileDAO();

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<ApplicationProfile> getApplicationProfiles()
    {
        return applicationProfileDAO.getAll();
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationProfile addApplicationProfile( ApplicationProfile applicationProfile )
    {
        applicationProfile.setId( null );
        applicationProfileDAO.insert( applicationProfile );

        return applicationProfile;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicationProfile updateApplicationProfile( ApplicationProfile applicationProfile )
    {
        applicationProfileDAO.update( applicationProfile );

        return applicationProfile;
    }

    @DELETE
    @Path( "{applicationProfileId}" )
    public void deleteApplicationProfile( @PathParam( "applicationProfileId" ) String applicationProfileId )
    {
        ApplicationProfile applicationProfile = applicationProfileDAO.getByIdNotNull( applicationProfileId );

        applicationProfileDAO.delete( applicationProfile );
    }
}
