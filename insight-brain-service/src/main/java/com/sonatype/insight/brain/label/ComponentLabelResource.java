/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;

@Path( ComponentLabelResource.SERVICE_PATH )
public class ComponentLabelResource
{
    public static final String SERVICE_PATH = "rest/label/component/{applicationPublicId}/{hash}";

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LabelDAO labelDAO = new LabelDAO();

    private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

    @PUT
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void setComponentLabels( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                    @PathParam( "hash" ) String hash, Set<String> stringLabels )
    {
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        componentLabelDAO.setComponentLabels( application.getId(), hash, stringLabels );
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Label> getComponentLabels( @PathParam( "appId" ) String applicationPublicId,
                                           @PathParam( "hash" ) String hash )
    {
        Application application = applicationDAO.getOrInsertByPublicId( applicationPublicId );
        List<ComponentLabel> labelIds = componentLabelDAO.getByApplicationIdAndHash( application.getId(), hash );
        List<Label> labels = new ArrayList<Label>( labelIds.size() );
        for ( ComponentLabel labelId : labelIds )
        {
            Label label = labelDAO.getById( labelId.getLabelId() );
            if ( label != null )
            {
                labels.add( label );
            }
        }
        return labels;
    }
}
