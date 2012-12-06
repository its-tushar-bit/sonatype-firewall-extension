/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;

@Path( LabelResource.SERVICE_PATH )
public class LabelResource
{
    public static final String SERVICE_PATH = "/rest/label/application/{appId}";

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LabelDAO labelDAO = new LabelDAO();

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<Label> getLabels( @PathParam( "appId" ) String applicationPublicId )
    {
        Application application = applicationDAO.getOrInsertByPublicId( applicationPublicId );

        return labelDAO.getByApplicationId( application.getId() );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Label addLabel( @PathParam( "appId" ) String applicationPublicId, Label label )
    {
        Application application = applicationDAO.getOrInsertByPublicId( applicationPublicId );

        label.setId( null );
        label.setApplicationId( application.getId() );
        label.fixLabelLowercase();
        labelDAO.insert( label );

        return label;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Label updateLabel( @PathParam( "appId" ) String applicationPublicId, Label label )
    {
        applicationDAO.getOrInsertByPublicId( applicationPublicId );

        label.fixLabelLowercase();
        labelDAO.update( label );

        return label;
    }

    @DELETE
    @Path( "{labelId}" )
    public void deleteLabel( @PathParam( "appId" ) String applicationPublicId, @PathParam( "labelId" ) String labelId )
    {
        applicationDAO.getOrInsertByPublicId( applicationPublicId );

        Label label = labelDAO.getById( labelId );
        labelDAO.delete( label );
    }
}
