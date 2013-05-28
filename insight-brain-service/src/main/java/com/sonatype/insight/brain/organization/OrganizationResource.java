/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;

@Named
@Path( OrganizationResource.SERVICE_PATH )
public class OrganizationResource
{
    public static final String SERVICE_PATH = "rest/organization";

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Organization addOrganization( Organization organization )
    {
        new OrganizationDAO().insert( organization );

        return organization;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Organization updateOrganization( Organization organization )
    {
        new OrganizationDAO().update( organization );

        return organization;
    }
}
