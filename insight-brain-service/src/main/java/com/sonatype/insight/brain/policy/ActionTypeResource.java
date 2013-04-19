/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collection;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;

@Named
@Path( ActionTypeResource.SERVICE_PATH )
public class ActionTypeResource
{
    public static final String SERVICE_PATH = "rest/policy/actionType";

    private static final Logger log = LoggerFactory.getLogger( ActionTypeResource.class );

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Collection<ActionType> getActionTypes()
    {
        log.debug( "Received request to get all action types" );

        return ActionTypes.getAll();
    }
}
