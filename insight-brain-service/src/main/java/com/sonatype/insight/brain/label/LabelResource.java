/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path( LabelResource.SERVICE_PATH )
public class LabelResource
{
    public static final String SERVICE_BASEPATH = "rest/label/";

    public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}";

    @Context
    private InsightWork work;

    private LabelDAO labelDAO = new LabelDAO();

    /**
     * @since 1.6
     */
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<Label> getLabels( @PathParam( "ownerType" ) String ownerType, @PathParam( "ownerId" ) String ownerId )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        return labelDAO.getByOwnerId( ownerId );
    }

    /**
     * @since 1.6
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Label addLabel( @PathParam( "ownerType" ) String ownerType, @PathParam( "ownerId" ) String ownerId,
                           Label label )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        label.setId( null );
        label.setOwnerId( ownerId );
        label.fixLabelLowercase();
        labelDAO.insert( label );

        return label;
    }

    /**
     * @since 1.6
     */
    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Label updateLabel( @PathParam( "ownerType" ) String ownerType, @PathParam( "ownerId" ) String ownerId,
                              Label label )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        label.setOwnerId( ownerId );
        label.fixLabelLowercase();
        labelDAO.update( label );

        return label;
    }

    /**
     * @since 1.6
     */
    @DELETE
    @Path( "{labelId}" )
    public void deleteLabel( @PathParam( "ownerType" ) String ownerType, @PathParam( "ownerId" ) String ownerId,
                             @PathParam( "labelId" ) String labelId )
    {
        String internalOwnerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        Label label = labelDAO.getById( labelId );
        if ( label == null )
        {
            throw new NotFoundException( "Cannot find a label with id " + labelId );
        }
        if ( !internalOwnerId.equals( label.getOwnerId() ) )
        {
            throw new NotFoundException( "Cannot find a label with id " + labelId + " for " + ownerType + " id "
                + ownerId );
        }

        // Verify that the label is not used in a policy condition
        PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );
        for ( Policy policy : policyDAO.getByOwnerId( internalOwnerId ) )
        {
            for ( Constraint constraint : policy.getConstraints() )
            {
                for ( Condition condition : constraint.getConditions() )
                {
                    if ( LabelConditionType.ID.equals( condition.getConditionTypeId() )
                        && labelId.equals( condition.getValue() ) )
                    {
                        // The label is used in a policy condition
                        throw new BadRequestException(
                                                       "Cannot delete the label because it is used in a condition for the '"
                                                           + policy.getName() + "' policy" );
                    }
                }
            }
        }

        labelDAO.delete( label );
    }
}
