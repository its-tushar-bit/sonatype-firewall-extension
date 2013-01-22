/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.error.exception.BadRequestException;

@Path( ComponentLabelResource.SERVICE_PATH )
public class ComponentLabelResource
{
    public static final String SERVICE_PATH = "rest/label/component/{applicationPublicId}/{hash}";

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LabelDAO labelDAO = new LabelDAO();

    private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public List<Label> setComponentLabels( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                           @PathParam( "hash" ) String hash, ComponentLabelState data )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        if ( data.getLabels() != null )
        {
            for ( String label : data.getLabels() )
            {
                if ( label.length() > 50 )
                {
                    throw new BadRequestException( "The label '" + label
                        + "' exceeds the maximum length of 50 characters" );
                }
            }
        }

        componentLabelDAO.setComponentLabels( application.getId(), hash, data.getLabels(), data.getColor() );

        return labelDAO.getByApplicationIdAndHash( application.getId(), hash );
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Label> getComponentLabels( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                           @PathParam( "hash" ) String hash )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        return labelDAO.getByApplicationIdAndHash( application.getId(), hash );
    }
}
