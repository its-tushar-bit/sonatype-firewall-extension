/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.model.Application;

@Path( ComponentLabelResource.SERVICE_PATH )
public class ComponentLabelResource
{
    public static final String SERVICE_PATH = "rest/label/component/{appId}/{hash}";

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

    @PUT
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void setComponentLabels( @PathParam( "appId" ) String applicationPublicId, @PathParam( "hash" ) String hash,
                                    Set<String> stringLabels )
    {
        Application application = applicationDAO.getOrInsertByPublicId( applicationPublicId );

        componentLabelDAO.setComponentLabels( application.getId(), hash, stringLabels );
    }
}
