/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationProfilePolicyDAO;
import com.sonatype.insight.brain.model.ApplicationProfilePolicy;

@Path( ApplicationProfilePolicyResource.SERVICE_PATH )
public class ApplicationProfilePolicyResource
{
    public static final String SERVICE_PATH = "rest/applicationProfilePolicy/{applicationProfileId}";

    private ApplicationProfilePolicyDAO applicationProfilePolicyDAO = new ApplicationProfilePolicyDAO();

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<ApplicationProfilePolicy> getApplicationProfilePolicies( @PathParam( "applicationProfileId" ) String applicationProfileId )
    {
        return applicationProfilePolicyDAO.getByApplicationProfileId( applicationProfileId );
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public List<ApplicationProfilePolicy> setApplicationProfilePolicies( @PathParam( "applicationProfileId" ) String applicationProfileId,
                                                                         Set<String> policyIds )
    {
        applicationProfilePolicyDAO.set( applicationProfileId, policyIds );

        return applicationProfilePolicyDAO.getByApplicationProfileId( applicationProfileId );
    }
}
