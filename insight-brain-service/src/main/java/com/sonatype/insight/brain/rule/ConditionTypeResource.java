/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.rule.AllConditionTypes;
import com.sonatype.insight.brain.model.rule.ConditionType;

@Path( ConditionTypeResource.SERVICE_PATH )
public class ConditionTypeResource
{
    public static final String SERVICE_PATH = "/rest/policy/conditionType";

    private static final Logger log = LoggerFactory.getLogger( ConditionTypeResource.class );

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Collection<ConditionType> getConditionTypes()
    {
        log.debug( "Received request to get all condition types" );

        return AllConditionTypes.getAll();
    }
}
