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

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;

@Path( ConditionValueTypeResource.SERVICE_PATH )
public class ConditionValueTypeResource
{
    public static final String SERVICE_PATH = "rest/conditionValueType/{applicationPublicId}";

    private static final Logger log = LoggerFactory.getLogger( ConditionValueTypeResource.class );

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public Collection<ConditionValueType> getConditionValueTypes( @PathParam( "applicationPublicId" ) String applicationPublicId )
    {
        log.debug( "Received request to get all condition value types for application ID {}", applicationPublicId );

        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        return (Collection) ConditionValueTypes.getAll( application.getId() );
    }
}
