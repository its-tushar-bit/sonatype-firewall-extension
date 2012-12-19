/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightWork;

@Path( PolicyResource.SERVICE_PATH )
public class PolicyResource
{
    public static final String SERVICE_PATH = "rest/policy/{appId}";

    private static final Logger log = LoggerFactory.getLogger( PolicyResource.class );

    @Context
    private InsightWork work;

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Policy> getPolicies( @PathParam( "appId" ) final String appId )
    {
        log.debug( "Received request to get all policies for appId {}", appId );
        return policyDAO().getByApplicationId( appId );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Policy addPolicy( @PathParam( "appId" ) final String appId, final Policy policy )
    {
        log.debug( "Received request to add policy for appId {}", appId );
        return policyDAO().insert( appId, policy );
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Policy updatePolicy( @PathParam( "appId" ) final String appId, final Policy policy )
    {
        log.debug( "Received request to update policy for appId {}, policyId {}", appId, policy.getId() );
        return policyDAO().update( appId, policy );
    }

    @DELETE
    @Path( "{policyId}" )
    public void deletePolicy( @PathParam( "appId" ) final String appId, @PathParam( "policyId" ) final String policyId )
    {
        log.debug( "Received request to delete policy for appId {}, policyId {}", appId, policyId );
        policyDAO().delete( appId, policyId );
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }
}
