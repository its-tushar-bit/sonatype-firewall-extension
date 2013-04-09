/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;

@Path( ConditionValueTypeResource.SERVICE_PATH )
public class ConditionValueTypeResource
{
    public static final String SERVICE_PATH = "rest/conditionValueType/{policyOwnerId}";

    private static final Logger log = LoggerFactory.getLogger( ConditionValueTypeResource.class );

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public Collection<ConditionValueType> getConditionValueTypes( @PathParam( "policyOwnerId" ) String policyOwnerId )
    {
        log.debug( "Received request to get all condition value types for policyOwnerId ID {}", policyOwnerId );

        String internalPolicyOwnerId = PolicyResource.getInternalPolicyOwnerId( policyOwnerId );
        return (Collection) ConditionValueTypes.getAll( internalPolicyOwnerId );
    }
}
