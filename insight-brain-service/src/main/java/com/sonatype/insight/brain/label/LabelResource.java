/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import javax.persistence.EntityManager;
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

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

@Path( LabelResource.SERVICE_PATH )
public class LabelResource
{
    public static final String SERVICE_PATH = "rest/label/application/{applicationPublicId}";

    @Context
    private InsightWork work;

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LabelDAO labelDAO = new LabelDAO();

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<Label> getLabels( @PathParam( "applicationPublicId" ) String applicationPublicId )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        return labelDAO.getByApplicationId( application.getId() );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Label addLabel( @PathParam( "applicationPublicId" ) String applicationPublicId, Label label )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        label.setId( null );
        label.setApplicationId( appId );
        label.fixLabelLowercase();
        EntityManager em = labelDAO.createEntityManager();
        try
        {
            if ( labelDAO.getByApplicationIdAndLowercaseLabel( em, appId, label.getLabelLowercase() ) != null )
            {
                throw new ConflictException( "A label with the same name already exists" );
            }
            labelDAO.insert( label );
        }
        finally
        {
            LabelDAO.close( em );
        }

        return label;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Label updateLabel( @PathParam( "applicationPublicId" ) String applicationPublicId, Label label )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        label.setApplicationId( appId );
        label.fixLabelLowercase();
        EntityManager em = labelDAO.createEntityManager();
        try
        {
            Label otherLabel = labelDAO.getByApplicationIdAndLowercaseLabel( em, appId, label.getLabelLowercase() );
            if ( otherLabel != null && !otherLabel.getId().equals( label.getId() ) )
            {
                throw new ConflictException( "A label with the same name already exists" );
            }
            labelDAO.update( label );
        }
        finally
        {
            LabelDAO.close( em );
        }

        return label;
    }

    @DELETE
    @Path( "{labelId}" )
    public void deleteLabel( @PathParam( "applicationPublicId" ) String applicationPublicId,
                             @PathParam( "labelId" ) String labelId )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String appId = application.getId();

        Label label = labelDAO.getById( labelId );
        if ( label == null )
        {
            throw new NotFoundException( "Cannot find a label with id " + labelId );
        }
        if ( !appId.equals( label.getApplicationId() ) )
        {
            throw new NotFoundException( "Cannot find a label with id " + labelId + " for application id "
                + applicationPublicId );
        }

        // Verify that the label is not used in a policy condition
        PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );
        for ( Policy policy : policyDAO.getByApplicationId( appId ) )
        {
            for ( Constraint constraint : policy.getConstraints() )
            {
                for ( Condition condition : constraint.getConditions() )
                {
                    if ( LabelConditionType.ID.equals( condition.getConditionTypeId() )
                        && labelId.equals( condition.getValue() ) )
                    {
                        // The label is used in a policy condition
                        throw new BadRequestException( "Cannot delete a label used in a policy condition" );
                    }
                }
            }
        }

        labelDAO.delete( label );
    }
}
