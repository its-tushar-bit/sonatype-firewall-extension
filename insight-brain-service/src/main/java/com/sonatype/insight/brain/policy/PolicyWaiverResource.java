/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.6
 */
@Named
@Path( PolicyWaiverResource.SERVICE_PATH )
public class PolicyWaiverResource
{
    public static final String SERVICE_BASEPATH = "rest/policyWaiver/";

    public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}";

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public PolicyWaiver addPolicyWaiver( @PathParam( "ownerType" ) String ownerType,
                                         @PathParam( "ownerId" ) String ownerId, PolicyWaiver policyWaiver )
    {
        String internalOwnerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        policyWaiver.setId( null );
        policyWaiver.setOwnerId( internalOwnerId );
        new PolicyWaiverDAO().insert( policyWaiver );
        return policyWaiver;
    }

    @DELETE
    @Path( "{policyWaiverId}" )
    public void deletePolicyWaiver( @PathParam( "ownerType" ) String ownerType, @PathParam( "ownerId" ) String ownerId,
                                    @PathParam( "policyWaiverId" ) String policyWaiverId )
    {
        String internalOwnerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
        PolicyWaiver policyWaiver = policyWaiverDAO.getById( policyWaiverId );
        if ( policyWaiver == null )
        {
            return;
        }
        if ( !internalOwnerId.equals( policyWaiver.getOwnerId() ) )
        {
            throw new NotFoundException( "Cannot find a policy waiver with id " + policyWaiverId + " for " + ownerType
                + " id " + ownerId );
        }

        policyWaiverDAO.delete( policyWaiver );
    }

    @GET
    @Path( "component/{hash}" )
    @Produces( MediaType.APPLICATION_JSON )
    public List<PolicyWaiver> getPolicyWaiversByHash( @PathParam( "ownerType" ) String ownerType,
                                                      @PathParam( "ownerId" ) String ownerId,
                                                      @PathParam( "hash" ) String hash )
    {
        String internalOwnerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
        return policyWaiverDAO.getByOwnerIdHash( internalOwnerId, hash, true );
    }
}
