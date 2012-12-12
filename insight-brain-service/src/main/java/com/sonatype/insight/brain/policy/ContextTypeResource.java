/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.model.policy.ContextType;
import com.sonatype.insight.brain.model.policy.contexts.ContextTypes;

@Path( ContextTypeResource.SERVICE_PATH )
public class ContextTypeResource
{
    public static final String SERVICE_PATH = "rest/policy/contextType";

    private static final Logger log = LoggerFactory.getLogger( ContextTypeResource.class );

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Collection<ContextType> getContextTypes()
    {
        log.debug( "Received request to get all context types" );

        return ContextTypes.getAll();
    }
}
